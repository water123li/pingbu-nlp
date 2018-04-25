package pingbu.common;

/**
 * Redirect log to null
 * 
 * @author pingbu
 */
public class NullLogger implements ILogger {
    public void d(final String tag, final String info) {
    }

    public void e(final String tag, final String info) {
    }
}
