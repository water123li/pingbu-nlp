package pingbu.common;

/**
 * 空日志实现类
 * 
 * @author pingbu
 */
public class NullLogger implements ILogger {
    @Override
    public void d(final String tag, final String info) {
    }

    @Override
    public void e(final String tag, final String info) {
    }
}
