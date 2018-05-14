package pingbu.common;

import java.io.*;

public class JarStorage implements Storage {
    private final ClassLoader mClassLoader;

    public JarStorage(final ClassLoader classLoader) {
        mClassLoader = classLoader;
    }

    @Override
    public boolean exists(String name) throws IOException {
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
