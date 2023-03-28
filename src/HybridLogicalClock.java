import java.util.Comparator;

/**
 * 混合逻辑时钟 数据类
 * HLC时间戳以64位整型表示
 * 保留位：2    物理时钟位：46    逻辑时钟位：16
 * 物理时钟以1毫秒位单位
 * 即1毫秒内逻辑时钟最多可以前进65535次
 * -------------------------------------------------
 * <a href="https://jaredforsyth.com/posts/hybrid-logical-clocks/">
 * [1]有文章提到还需要第三个参数，即每个设备唯一的节点ID，如恰巧多个设备之间生成的HLC时间戳相同，
 * 可以以此断开连接</a>
 * -------------------------------------------------
 */
public record HybridLogicalClock(HLCTimestamp timestamp) {
    /**
     *
     * @param wallTime 物理时钟
     * @param logicalTime 逻辑时钟
     * @return HLC 混合逻辑时钟
     */
    public static HybridLogicalClock getInstance(Long wallTime, Long logicalTime) {
        // 将wallTime左移16位，低16位改为逻辑时钟
        return new HybridLogicalClock(HLCTimestamp.getInstance(wallTime, logicalTime));
    }

    /**
     * 原语 ClockCurrent
     * 如果本地物理时钟值大于HLC时间戳，则覆盖HLC时间戳，并返回
     * @return 新的HLC
     */
    public HybridLogicalClock ClockCurrent() {
        var localWallTime = System.currentTimeMillis();
        if (localWallTime > this.timestamp.getWallTime()) {
            // 本地物理时钟大于HLC时间戳的物理时钟部分，则覆盖HLC时间戳，逻辑时钟归零，并返回
            return HybridLogicalClock.getInstance(localWallTime, 0L);
        }
        return this;
    }

    /**
     * 原语 ClockAdvance
     * 如果本地物理时钟值大于HLC时间戳，则覆盖HLC时间戳，逻辑时钟归零，否则逻辑时钟+1后返回
     * @return 新的HLC
     */
    public HybridLogicalClock ClockAdvance() {
        var localWallTime = System.currentTimeMillis();
        if (localWallTime > this.timestamp.getWallTime()) {
            return HybridLogicalClock.getInstance(localWallTime, 0L);
        }
        return HybridLogicalClock.getInstance(this.timestamp.getWallTime(), this.timestamp.getLogicalTime() + 1);
    }

    /**
     * 原语 ClockUpdate
     * 通过e事件的时间戳更新本地HLC时间戳，取最大值
     * @param e 事件
     * @return 新的HLC
     */
    public HybridLogicalClock ClockUpdate(HybridLogicalClock e) {
        var localWallTime = System.currentTimeMillis();
        if (localWallTime > this.timestamp.getWallTime() && localWallTime > e.timestamp.getWallTime()) {
            return HybridLogicalClock.getInstance(localWallTime, 0L);
        }

        if (e.timestamp.compareTo(this.timestamp) > 0) {
            return HybridLogicalClock.getInstance(e.timestamp.getWallTime(), e.timestamp.getLogicalTime() + 1);
        } else if (e.timestamp.compareTo(this.timestamp) < 0) {
            return HybridLogicalClock.getInstance(this.timestamp.getWallTime(), this.timestamp.getLogicalTime() + 1);
        }
        // TODO [1]需要设备ID排除该情况
        return null;
    }

    @Override
    public String toString() {
        return "HybridLogicalClock{\n" +
                "timestamp=" + timestamp + "\n" +
                "wallTime=" + timestamp.getWallTime() + "\n" +
                "logicalTime=" + timestamp.getLogicalTime() + "\n" +
                '}';
    }

    public record HLCTimestamp(Long value) implements Comparable<HLCTimestamp> {
        public static HLCTimestamp getInstance(Long wallTime, Long logicalTime) {
            return new HLCTimestamp((wallTime << 16) | (logicalTime & 65535));
        }

        public Long getWallTime() {
            return value >> 16;
        }

        public Long getLogicalTime() {
            return value & 65535;
        }

        @Override
        public String toString() {
            return Long.toBinaryString(value);
        }

        /**
         * 重载比较器
         * 先比较物理时钟大小，后比较逻辑时钟大小
         * @param o the object to be compared.
         * @return 比较结果
         */
        @Override
        public int compareTo(HLCTimestamp o) {
            return Comparator.comparingLong(HLCTimestamp::getWallTime)
                    .thenComparingLong(HLCTimestamp::getLogicalTime)
                    .compare(this, o);
        }
    }
}

