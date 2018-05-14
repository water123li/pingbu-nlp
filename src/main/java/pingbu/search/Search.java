package pingbu.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pingbu.common.Logger;
import pingbu.search.Index.Iterator;

/**
 * 搜索
 * 
 * @author pingbu
 */
public class Search {
    private static final String TAG = Search.class.getSimpleName();
    private static final double THRESHOLD = .5;

    private final List<Map<String, String>> mItems = new ArrayList<Map<String, String>>();
    private final Map<String, Index> mIndexes = new HashMap<String, Index>();

    protected Index getIndex(String field) {
        return mIndexes.get(field);
    }

    public void addField(final String name, final Index index) {
        mIndexes.put(name, index);
    }

    public void addItem(final Map<String, String> item) {
        final int id = mItems.size();
        mItems.add(item);
        for (final Map.Entry<String, Index> index : mIndexes.entrySet())
            index.getValue().addItem(id, item.get(index.getKey()));
    }

    public int getItemCount() {
        return mItems.size();
    }

    public Map<String, String> getItem(final int id) {
        return mItems.get(id);
    }

    public class Result {
        public final int id;
        public final double score;

        private Result(final int id, final double score) {
            this.id = id;
            this.score = score;
        }

        public Map<String, String> getItem() {
            return mItems.get(id);
        }

        public Object getField(final String name) {
            return mItems.get(id).get(name);
        }
    }

    public static String formatItem(final Map<String, String> item) {
        if (item.isEmpty())
            return "";
        final StringBuilder s = new StringBuilder();
        for (final Map.Entry<String, String> field : item.entrySet()) {
            s.append(',');
            s.append(field.getKey());
            s.append('=');
            s.append(field.getValue());
        }
        return s.substring(1);
    }

    public Collection<Result> search(final Map<String, String> conditions,
            final int limit) {
        Logger.d(TAG, "==> search " + formatItem(conditions));
        final List<Result> results = new ArrayList<Result>();
        final List<Iterator> iterators = new ArrayList<Iterator>();
        for (final Map.Entry<String, String> condition : conditions.entrySet()) {
            final String value = condition.getValue();
            if (value != null && !value.isEmpty()) {
                final String field = condition.getKey();
                final Index index = mIndexes.get(field);
                if (index != null)
                    iterators.add(index.iterate(value));
            }
        }
        for (;;) {
            int item = Integer.MAX_VALUE;
            for (final Iterator iterator : iterators) {
                final int i = iterator.getNextItem();
                if (i < item)
                    item = i;
            }
            if (item >= Integer.MAX_VALUE)
                break;
            double score = 1.;
            for (final Iterator iterator : iterators)
                score *= iterator.sumupToItem(item);
            if (score >= THRESHOLD)
                results.add(new Result(item, score));
        }
        Collections.sort(results, new Comparator<Result>() {
            @Override
            public int compare(final Result r0, final Result r1) {
                return r0.score < r1.score ? 1 : r0.score == r1.score ? 0 : -1;
            }
        });
        while (results.size() > limit)
            results.remove(results.size() - 1);
        Logger.d(TAG, results.size() + " results <== search");
        return results;
    }
}
