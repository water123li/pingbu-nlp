package pingbu.common;

/**
 * 简单的日志类
 * 
 * @author pingbu
 */
public abstract class Logger {
    private static ILogger mLogger = new ConsoleLogger(false);

    public static void setLogger(final ILogger logger) {
        mLogger = logger;
    }

    public static void d(final String tag, final String info) {
        mLogger.d(tag, info);
    }

    public static void e(final String tag, final String info) {
        mLogger.e(tag, info);
    }
}
