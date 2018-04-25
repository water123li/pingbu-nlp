package pingbu.common;

/**
 * 简单的日志类
 * 
 * @author pingbu
 */
public class MyLog {
    private static final boolean LOG_DISPLAY = false;

    public static void logD(String tag, String info) {
        if (LOG_DISPLAY)
            System.out.printf("[%s] %s\n", tag, info);
    }

    public static void logE(String tag, String info) {
        System.err.printf("[%s] %s\n", tag, info);
    }
}
