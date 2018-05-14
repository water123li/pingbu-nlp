package pingbu.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import pingbu.common.FileStorage;
import pingbu.common.Storage;

public abstract class SearchFile {
    private static final String BOM = "\uFEFF";

    public static Search load(final String path) {
        final String fullPath = new File(path).getAbsolutePath();
        final int p = fullPath.lastIndexOf(File.separatorChar) + 1;
        final Storage storage = new FileStorage(fullPath.substring(0, p));
        return load(storage, fullPath.substring(p));
    }

    public static Search load(final Storage storage, final String fileName) {
        try (final InputStream s = storage.open(fileName)) {
            final BufferedReader in = new BufferedReader(new InputStreamReader(
                    s, "UTF-8"));
            final Search search = new Search();
            final String[] fields;
            {
                String l = in.readLine();
                if (l.startsWith(BOM))
                    l = l.substring(BOM.length());
                fields = l.split(",");
                for (int i = 0; i < fields.length; ++i) {
                    final String field = fields[i];
                    final String name = field.substring(2);
                    final Index fieldIndex;
                    if (field.startsWith("S:"))
                        fieldIndex = new IndexSearch();
                    else if (field.startsWith("T:"))
                        fieldIndex = new IndexText();
                    else if (field.startsWith("V:"))
                        fieldIndex = new IndexInteger();
                    else
                        fieldIndex = null;
                    if (fieldIndex != null) {
                        fields[i] = name;
                        search.addField(name, fieldIndex);
                    }
                }
            }
            for (;;) {
                final String l = in.readLine();
                if (l == null)
                    break;
                final Map<String, String> item = new HashMap<String, String>();
                final String[] values = l.split(",");
                for (int i = 0; i < fields.length && i < values.length; ++i) {
                    String value = values[i];
                    if (value.startsWith("\"") && value.endsWith("\""))
                        value = value.substring(1, value.length() - 1)
                                .replace("\\\"", "\"").replace("\\\\", "\\");
                    item.put(fields[i], value);
                }
                search.addItem(item);
            }
            return search;
        } catch (final IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
