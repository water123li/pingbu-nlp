package pingbu.search;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import pingbu.common.Logger;

class SqlIterator implements SearchIndex.Iterator {
    private static final String TAG = SqlIterator.class.getSimpleName();

    private final Connection mConnection;
    private final Statement mStatement;
    private final ResultSet mResultSet;
    private int mNextId = Integer.MAX_VALUE;

    private void __cacheNext() {
        try {
            if (mResultSet.next())
                mNextId = mResultSet.getInt(1);
            else
                mNextId = Integer.MAX_VALUE;
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public SqlIterator(final String connStr, final String sql) {
        Logger.d(TAG, sql);
        Connection conn = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            conn = DriverManager.getConnection(connStr);
            st = conn.createStatement();
            rs = st.executeQuery(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        mConnection = conn;
        mStatement = st;
        mResultSet = rs;
        __cacheNext();
    }

    @Override
    public int getNextItem() {
        return mNextId;
    }

    @Override
    public double sumUpToItem(final int id) {
        if (getNextItem() == id) {
            __cacheNext();
            return 1;
        }
        return 0;
    }

    @Override
    public void close() {
        if (mResultSet != null)
            try {
                mResultSet.close();
            } catch (SQLException e) {
            }
        if (mStatement != null)
            try {
                mStatement.close();
            } catch (SQLException e) {
            }
        if (mConnection != null)
            try {
                mConnection.close();
            } catch (SQLException e) {
            }
    }
}
