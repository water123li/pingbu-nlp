package pingbu.search;

/**
 * 搜索结果
 * 
 * @author pingbu
 */
public class SearchResult {
    /**
     * 条目id
     */
    public final int id;

    /**
     * 条目得分，满分1.0
     */
    public final double score;

    SearchResult(final int id, final double score) {
        this.id = id;
        this.score = score;
    }
}
