package pingbu.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndexText implements Index {

    private final Map<String, List<Integer>> mValueToIds = new HashMap<String, List<Integer>>();

    @Override
    public void addItem(final int id, final String value) {
        List<Integer> ids = mValueToIds.get(value);
        if (ids == null) {
            ids = new ArrayList<Integer>();
            mValueToIds.put(value, ids);
        }
        ids.add(id);
    }

    @Override
    public ListIterator iterate(final String value) {
        return new ListIterator(mValueToIds.get(value));
    }
}
