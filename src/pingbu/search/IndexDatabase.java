package pingbu.search;

import java.util.ArrayList;
import java.util.List;

public class IndexDatabase implements Index {

    private static void __appendValue(final StringBuilder s, String value,
            String format) {
        if (format == null || format.equalsIgnoreCase("String")) {
            s.append('\"');
            s.append(value.replace("\"", "\"\""));
            s.append('\"');
        } else if (format.equalsIgnoreCase("Bool")
                || format.equalsIgnoreCase("Boolean")) {
            s.append(Boolean.parseBoolean(value) ? "1" : "0");
        } else {
            s.append(value);
        }
    }

    private static String __formatSQL(String fmt, String value) {
        final String[] values = value.split(",");
        final StringBuilder sql = new StringBuilder();
        for (int p = 0;;) {
            final int a = fmt.indexOf('{', p) + 1;
            if (a <= 0) {
                sql.append(fmt.substring(p));
                break;
            }
            final int b = fmt.indexOf('}', a + 1);
            if (b < 0)
                throw new RuntimeException();
            sql.append(fmt.substring(p, a - 1));
            final String slot = fmt.substring(a, b);
            final int q = slot.indexOf(':');
            if (q < 0)
                __appendValue(sql, values[Integer.parseInt(slot)], null);
            else
                __appendValue(sql,
                        values[Integer.parseInt(slot.substring(0, q))],
                        slot.substring(q + 1));
            p = b + 1;
        }
        return sql.toString();
    }

    private final String mConnStr, mSqlFmt;

    public IndexDatabase(String connStr, String sqlFmt) {
        mConnStr = connStr;
        mSqlFmt = sqlFmt;
    }

    @Override
    public void addItem(final int id, final String value) {
        throw new RuntimeException();
    }

    @Override
    public Iterator iterate(final String value) {
        final String[] values = value.split("\\|");
        if (values.length == 1)
            return new DatabaseIterator(mConnStr, __formatSQL(mSqlFmt, value));
        final List<Index.Iterator> iterators = new ArrayList<Index.Iterator>();
        for (String v : values)
            iterators.add(new DatabaseIterator(mConnStr,
                    __formatSQL(mSqlFmt, v)));
        return new MultiIterator(iterators);
    }
}
