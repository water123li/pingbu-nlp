package pingbu.common;

/**
 * Redirect log to console
 * 
 * @author pingbu
 */
public class ConsoleLogger implements ILogger {
    private final boolean mLogDebug;

    public ConsoleLogger(final boolean logDebug) {
        mLogDebug = logDebug;
    }

    public void d(final String tag, final String info) {
        if (mLogDebug)
            System.out.printf("[%s] %s\n", tag, info);
    }

    public void e(final String tag, final String info) {
        System.err.printf("[%s] %s\n", tag, info);
    }
}
