public class Main {
    private static boolean testHLCTimestamp() {
        long wallTime = 1679924536986L;
        var early = HybridLogicalClock.getInstance(wallTime, 0L);
        var middle = HybridLogicalClock.getInstance(wallTime, 1L);
        var late = HybridLogicalClock.getInstance(wallTime + 1, 0L);
        System.out.println(early.timestamp());
        System.out.println(middle.timestamp());
        System.out.println(late.timestamp());
        var old = late.timestamp();
        var now = late.ClockCurrent().timestamp();
        System.out.println(now.getWallTime() - old.getWallTime());
        System.out.println(now.getLogicalTime());
        System.out.println("-----------------------------------------------");
        return early.timestamp().compareTo(middle.timestamp()) < 0 && middle.timestamp().compareTo(late.timestamp()) < 0;
    }

    public static void main(String[] args) {
        System.out.println(testHLCTimestamp());
        var now = System.currentTimeMillis();
        System.out.println("CurrentTimeMillis:" + now);
        var hlc = HybridLogicalClock.getInstance(now, 0L);
        System.out.println("[0]" + hlc);

        // 生成本地事件或发送消息事件
        for (int i=1;i<=20;i++) {
            hlc = hlc.ClockAdvance();
            System.out.println("[" + i + "]" + hlc);
        }

        // 接收消息事件更新
        var furtherTime = System.currentTimeMillis() + 200;
        System.out.println("--------------------------------");
        System.out.println(furtherTime);
        var otherHlc = HybridLogicalClock.getInstance(furtherTime, 100L);
        hlc = hlc.ClockUpdate(otherHlc);
        System.out.println("updated hlc:" + hlc);
        if (hlc == null) return;

        otherHlc = HybridLogicalClock.getInstance(now, 10000L);
        hlc = hlc.ClockUpdate(otherHlc);
        System.out.println("updated hlc:" + hlc);
        System.out.println("Cost Time(ms):" + (System.currentTimeMillis() - now));
    }
}