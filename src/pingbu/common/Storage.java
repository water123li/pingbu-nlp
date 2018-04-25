package pingbu.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Storage {
    OutputStream create(final String name) throws IOException;

    InputStream open(final String name) throws IOException;
}
