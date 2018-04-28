package pingbu.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IndexInteger implements Index {

    private final Map<Integer, List<Integer>> mValueToIds = new HashMap<Integer, List<Integer>>();

    @Override
    public void addItem(final int id, final String value) {
        final int intValue = Integer.parseInt(value);
        List<Integer> ids = mValueToIds.get(intValue);
        if (ids == null) {
            ids = new ArrayList<Integer>();
            mValueToIds.put(intValue, ids);
        }
        ids.add(id);
    }

    @Override
    public Index.Iterator iterate(final String value) {
        if (value.startsWith("range:")) {
            final int p = value.indexOf(',', 7);
            int a = Integer.parseInt(value.substring(7, p));
            int b = Integer
                    .parseInt(value.substring(p + 1, value.length() - 1));
            if (value.charAt(6) == '(')
                ++a;
            if (value.endsWith(")"))
                --b;
            final List<Index.Iterator> iterators = new ArrayList<Index.Iterator>();
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

    private static final class MultiIterator implements Index.Iterator {
        private final List<Index.Iterator> mIterators;

        public MultiIterator(final List<Index.Iterator> iterators) {
            mIterators = iterators;
        }

        @Override
        public int getNextItem() {
            int id = Integer.MAX_VALUE;
            for (final Index.Iterator iterator : mIterators)
                id = Math.min(id, iterator.getNextItem());
            return id;
        }

        @Override
        public double sumupToItem(final int id) {
            double score = 0;
            for (final Index.Iterator iterator : mIterators)
                score = Math.max(score, iterator.sumupToItem(id));
            return score;
        }
    }
}
