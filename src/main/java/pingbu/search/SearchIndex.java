package pingbu.search;

/**
 * 搜索索引
 */
public interface SearchIndex {

    /**
     * [private] 搜索索引迭代器，搜索引擎内部使用
     */
    interface Iterator extends AutoCloseable {
        int getNextItem();

        double sumUpToItem(int id);
    }

    /**
     * 为该索引添加条目
     *
     * @param id    条目id
     * @param value 条目该索引值
     */
    void addItem(int id, String value);

    /**
     * [private] 迭代搜索索引，搜索引擎内部使用
     *
     * @param value 搜索的索引值
     */
    Iterator iterate(String value);
}
