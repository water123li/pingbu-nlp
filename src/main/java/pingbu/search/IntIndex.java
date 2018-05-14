package pingbu.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数字搜索索引
 *
 * @author pingbu
 */
public class IntIndex implements SearchIndex {

    private final Map<Integer, Integer> mIdToValues = new HashMap<>();
    private final Map<Integer, List<Integer>> mValueToIds = new HashMap<>();

    @Override
    public void addItem(final int id, final String value) {
        final int intValue = Integer.parseInt(value);
        mIdToValues.put(id, intValue);
        List<Integer> ids = mValueToIds.get(intValue);
        if (ids == null) {
            ids = new ArrayList<>();
            mValueToIds.put(intValue, ids);
        }
        ids.add(id);
    }

    @Override
    public SearchIndex.Iterator iterate(final String value) {
        if (value.startsWith("range:")) {
            final int p = value.indexOf(',', 7);
            int a = Integer.parseInt(value.substring(7, p));
            int b = Integer
                    .parseInt(value.substring(p + 1, value.length() - 1));
            if (value.charAt(6) == '(')
                ++a;
            if (value.endsWith(")"))
                --b;
            final List<Iterator> iterators = new ArrayList<>();
            for (int intValue = a; intValue <= b; ++intValue) {
                final List<Integer> ids = mValueToIds.get(intValue);
                if (ids != null)
                    iterators.add(new ListIterator(ids));
            }
            return new MultiIterator(iterators);
        } else {
            final int intValue = Integer.parseInt(value);
            final List<Integer> ids = mValueToIds.get(intValue);
            return new ListIterator(ids);
        }
    }
}
