package pingbu.search;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

import pingbu.common.FileStorage;
import pingbu.common.Logger;
import pingbu.common.Storage;

public abstract class SearchLoader {
    private static final String TAG = SearchLoader.class.getSimpleName();
    private static final String BOM = "\uFEFF";

    /**
     * 从数据库加载搜索索引
     *
     * @param connStr 数据库连接串
     * @param sqlFmt  查询语句模板，要求查询字段1为id。
     *                模板至少带一个参数，形为{0}或{0:type}，type表示参数类型，支持string、int、bool，默认为string。
     *                类型int可以带两个参数，表示区间范围。
     * @return 搜索索引
     */
    public static SearchIndex loadIndexFromDB(final String connStr, final String sqlFmt) {
        return new SqlIndex(connStr, sqlFmt);
    }

    /**
     * 从数据库加载模糊搜索索引
     *
     * @param connStr 数据库连接串
     * @param sql     查询语句，要求查询字段1为id，查询字段2为待搜索的文本
     * @return 模糊搜索索引
     */
    public static FuzzyIndex loadFuzzyIndexFromDB(final String connStr, final String sql) {
        Logger.d(TAG, "==> loadFuzzyIndexFromDB");
        try {
            final FuzzyIndex index = new FuzzyIndex();
            try (final Connection conn = DriverManager.getConnection(connStr);
                 final Statement st = conn.createStatement();
                 final ResultSet rs = st.executeQuery(sql)) {
                while (rs.next())
                    index.addItem(rs.getInt(1), rs.getString(2));
            }
            return index;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            Logger.d(TAG, "<== loadFuzzyIndexFromDB");
        }
    }

    /**
     * 从CSV表格文件加载搜索库
     *
     * @param path CSV表格文件路径
     * @return 包含条目数据的搜索库
     */
    public static SearchLibraryWithData loadSearchLibraryFromCSV(final String path) {
        final String fullPath = new File(path).getAbsolutePath();
        final int p = fullPath.lastIndexOf(File.separatorChar) + 1;
        final Storage storage = new FileStorage(fullPath.substring(0, p));
        return loadSearchLibraryFromCSV(storage, fullPath.substring(p));
    }

    /**
     * 从CSV表格文件加载搜索库
     *
     * @param storage  CSV表格文件所在存储
     * @param fileName CSV表格文件名
     * @return 包含条目数据的搜索库
     */
    public static SearchLibraryWithData loadSearchLibraryFromCSV(final Storage storage, final String fileName) {
        try (final InputStream s = storage.open(fileName)) {
            final BufferedReader in = new BufferedReader(new InputStreamReader(s, "UTF-8"));
            final SearchLibraryWithData library = new SearchLibraryWithData();
            final String[] fields;
            {
                String l = in.readLine();
                if (l.startsWith(BOM))
                    l = l.substring(BOM.length());
                fields = l.split(",");
                for (int i = 0; i < fields.length; ++i) {
                    final String field = fields[i];
                    final String name = field.substring(2);
                    final SearchIndex fieldIndex;
                    if (field.startsWith("S:"))
                        fieldIndex = new FuzzyIndex();
                    else if (field.startsWith("T:"))
                        fieldIndex = new TextIndex();
                    else if (field.startsWith("V:"))
                        fieldIndex = new IntIndex();
                    else
                        fieldIndex = null;
                    if (fieldIndex != null) {
                        fields[i] = name;
                        library.addField(name, fieldIndex);
                    }
                }
            }
            for (int id = 1; ; ++id) {
                final String l = in.readLine();
                if (l == null)
                    break;
                final Map<String, String> item = new HashMap<>();
                final String[] values = l.split(",");
                for (int i = 0; i < fields.length && i < values.length; ++i) {
                    String value = values[i];
                    if (value.startsWith("\"") && value.endsWith("\""))
                        value = value.substring(1, value.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
                    item.put(fields[i], value);
                }
                library.addItem(id, item);
            }
            return library;
        } catch (final IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
