package pingbu.common;

/**
 * 日志实现接口
 */
public interface ILogger {
    /**
     * 打印调试信息
     *
     * @param tag  标签
     * @param info 信息文本
     */
    void d(String tag, String info);

    /**
     * 打印出错信息
     *
     * @param tag  标签
     * @param info 信息文本
     */
    void e(String tag, String info);
}
