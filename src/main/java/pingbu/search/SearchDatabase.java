package pingbu.search;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import pingbu.common.Logger;

public abstract class SearchDatabase {
    private static final String TAG = SearchDatabase.class.getSimpleName();

    public static IndexSearch loadSearchIndex(final String connStr,
            final String sql) {
        Logger.d(TAG, "==> loadSearchIndex");
        try {
            final IndexSearch index = new IndexSearch();
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
            Logger.d(TAG, "<== loadSearchIndex");
        }
    }
}
