package pingbu.common;

import java.io.*;

/**
 * Jar包文件存储类
 */
public class JarStorage implements Storage {
    private final ClassLoader mClassLoader;

    /**
     * 初始化Jar包文件存储类
     *
     * @param classLoader Jar包类加载器对象
     */
    public JarStorage(final ClassLoader classLoader) {
        mClassLoader = classLoader;
    }

    @Override
    public boolean exists(String name) {
        return mClassLoader.getResource(name) != null;
    }

    @Override
    public OutputStream create(final String name) throws IOException {
        throw new IOException("Can't write to jar");
    }

    @Override
    public InputStream open(final String name) throws IOException {
        return mClassLoader.getResourceAsStream(name);
    }

    @Override
    public void delete(String name) throws IOException {
        throw new IOException("Can't delete from jar");
    }
}
