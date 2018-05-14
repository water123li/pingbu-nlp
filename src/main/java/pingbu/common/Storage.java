package pingbu.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 文件存储接口
 */
public interface Storage {
    /**
     * 判断文件是否存在
     *
     * @param name 文件名
     * @return 是否存在
     */
    boolean exists(final String name);

    /**
     * 创建输出文件
     *
     * @param name 输出文件名
     * @return 输出文件流
     * @throws IOException 创建输出文件异常
     */
    OutputStream create(final String name) throws IOException;

    /**
     * 打开输入文件
     *
     * @param name 输入文件名
     * @return 输入文件流
     * @throws IOException 打开输入文件异常
     */
    InputStream open(final String name) throws IOException;

    /**
     * 删除文件
     *
     * @param name 待删除的文件名
     * @throws IOException 删除文件异常
     */
    void delete(final String name) throws IOException;
}
