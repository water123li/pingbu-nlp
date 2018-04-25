package pingbu.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileStreamBuilder implements StreamBuilder {
    private final String mRoot;

    public FileStreamBuilder(final String root) {
        if (root.endsWith(File.separator))
            mRoot = root;
        else
            mRoot = root + File.separatorChar;
    }

    @Override
    public OutputStream create(final String name) throws IOException {
        return new FileOutputStream(mRoot + name);
    }

    @Override
    public InputStream open(final String name) throws IOException {
        return new FileInputStream(mRoot + name);
    }
}
