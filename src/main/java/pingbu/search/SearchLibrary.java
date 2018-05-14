package pingbu.search;

import java.util.*;

import pingbu.common.Logger;

/**
 * 搜索库
 * 
 * @author pingbu
 */
public class SearchLibrary {
    private static final String TAG = SearchLibrary.class.getSimpleName();
    private static final double THRESHOLD = .5;

    private final Map<String, SearchIndex> mIndexes = new HashMap<>();

    /**
     * 添加一个索引字段
     *
     * @param name  字段名
     * @param index 字段索引
     */
    public void addField(final String name, final SearchIndex index) {
        mIndexes.put(name, index);
    }

    protected SearchIndex getField(final String name) {
        return mIndexes.get(name);
    }

    /**
     * 添加一个索引条目
     *
     * @param id   条目id
     * @param item 索引条目，为一组字段名与字段值的映射
     */
    public void addItem(final int id, final Map<String, String> item) {
        for (final Map.Entry<String, SearchIndex> index : mIndexes.entrySet())
            index.getValue().addItem(id, item.get(index.getKey()));
    }

    /**
     * 格式化条目
     *
     * @param item 条目，为一组字段名与字段值的映射
     * @return 描述条目的字符串
     */
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

    /**
     * 搜索
     *
     * @param conditions 搜索条件，为一组字段名与字段值的映射
     * @param start      搜索结果起始位置
     * @param limit      搜索结果范围
     * @return 搜索结果数组
     */
    public SearchResult[] search(final Map<String, String> conditions, final int start, final int limit) {
        Logger.d(TAG, "==> search " + formatItem(conditions));
        final List<SearchResult> results = new ArrayList<>();
        final List<SearchIndex.Iterator> iterators = new ArrayList<>();
        for (final Map.Entry<String, String> condition : conditions.entrySet()) {
            final String value = condition.getValue();
            if (value != null && !value.isEmpty()) {
                final String field = condition.getKey();
                final SearchIndex index = mIndexes.get(field);
                if (index != null)
                    iterators.add(index.iterate(value));
            }
        }
        for (; ; ) {
            int item = Integer.MAX_VALUE;
            for (final SearchIndex.Iterator iterator : iterators) {
                final int i = iterator.getNextItem();
                if (i < item)
                    item = i;
            }
            if (item >= Integer.MAX_VALUE)
                break;
            double score = 1.;
            for (final SearchIndex.Iterator iterator : iterators)
                score *= iterator.sumUpToItem(item);
            if (score >= THRESHOLD)
                results.add(new SearchResult(item, score));
        }
        Collections.sort(results, new Comparator<SearchResult>() {
            @Override
            public int compare(final SearchResult r0, final SearchResult r1) {
                return r0.score < r1.score ? 1 : r0.score == r1.score ? 0 : -1;
            }
        });
        final SearchResult[] rs = new SearchResult[Math.max(0, Math.min(results.size() - start, limit))];
        for (int i = 0; i < rs.length; ++i)
            rs[i] = results.get(start + i);
        Logger.d(TAG, rs.length + " results <== search");
        return rs;
    }
}
