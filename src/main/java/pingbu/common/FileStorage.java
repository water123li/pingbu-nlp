package pingbu.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileStorage implements Storage {
    private final String mRoot;

    public FileStorage(final String root) {
        if (root.endsWith(File.separator))
            mRoot = root;
        else
            mRoot = root + File.separatorChar;
    }

    @Override
    public boolean exists(String name) {
        return new File(mRoot + name).exists();
    }

    @Override
    public OutputStream create(final String name) throws IOException {
        return new FileOutputStream(mRoot + name);
    }

    @Override
    public InputStream open(final String name) throws IOException {
        return new FileInputStream(mRoot + name);
    }

    @Override
    public void delete(String name) throws IOException {
        if (!new File(mRoot + name).delete())
            throw new IOException("Failed to delete file '" + name + "'");
    }
}
