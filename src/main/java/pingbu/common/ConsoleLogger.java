package pingbu.common;

/**
 * 控制台日志实现类
 * 
 * @author pingbu
 */
public class ConsoleLogger implements ILogger {
    private final boolean mLogDebug;

    /**
     * 初始化控制台日志实现类
     *
     * @param logDebug 是否输出调试日志
     */
    public ConsoleLogger(final boolean logDebug) {
        mLogDebug = logDebug;
    }

    @Override
    public void d(final String tag, final String info) {
        if (mLogDebug)
            System.out.printf("[%s] %s\n", tag, info);
    }

    @Override
    public void e(final String tag, final String info) {
        System.err.printf("[%s] %s\n", tag, info);
    }
}
