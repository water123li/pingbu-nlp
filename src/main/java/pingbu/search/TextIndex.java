package pingbu.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文本搜索索引
 *
 * @author pingbu
 */
public class TextIndex implements SearchIndex {

    private final Map<Integer, String> mIdToValues = new HashMap<>();
    private final Map<String, List<Integer>> mValueToIds = new HashMap<String, List<Integer>>();

    @Override
    public void addItem(final int id, final String value) {
        mIdToValues.put(id, value);
        List<Integer> ids = mValueToIds.get(value);
        if (ids == null) {
            ids = new ArrayList<>();
            mValueToIds.put(value, ids);
        }
        ids.add(id);
    }

    @Override
    public ListIterator iterate(final String value) {
        return new ListIterator(mValueToIds.get(value));
    }
}
