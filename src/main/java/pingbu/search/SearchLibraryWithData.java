package pingbu.search;

import java.util.*;

/**
 * 包含条目数据的搜索库
 * 
 * @author pingbu
 */
public class SearchLibraryWithData extends SearchLibrary {

    private final Map<Integer, Map<String, String>> mItems = new HashMap<>();

    /**
     * 添加一个索引条目
     *
     * @param id   条目id
     * @param item 索引条目，为一组字段名与字段值的映射
     */
    public void addItem(final int id, final Map<String, String> item) {
        mItems.put(id, item);
        for (final Map.Entry<String, String> field : item.entrySet()) {
            final SearchIndex index = getField(field.getKey());
            if (index != null)
                index.addItem(id, field.getValue());
        }
    }

    /**
     * 添加一个索引条目
     *
     * @param id 条目id
     * @return 索引条目，为一组字段名与字段值的映射
     */
    public Map<String, String> getItem(final int id) {
        return mItems.get(id);
    }
}
