package pingbu.common;

/**
 * 简单的日志类
 * 
 * @author pingbu
 */
public abstract class Logger {
    private static ILogger mLogger = new ConsoleLogger(false);

    /**
     * 设置日志实现对象
     *
     * @param logger 日志实现对象
     */
    public static void setLogger(final ILogger logger) {
        mLogger = logger;
    }

    /**
     * 打印调试信息
     *
     * @param tag  标签
     * @param info 信息文本
     */
    public static void d(final String tag, final String info) {
        mLogger.d(tag, info);
    }

    /**
     * 打印出错信息
     *
     * @param tag  标签
     * @param info 信息文本
     */
    public static void e(final String tag, final String info) {
        mLogger.e(tag, info);
    }
}
