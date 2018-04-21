package pingbu.nlp;

import java.util.Collection;

/**
 * 词典基类，供语法搜索时引用
 * 
 * @author pingbu
 */
public abstract class Lexicon {

    public static final int TYPE_NORMAL = 0;
    public static final int TYPE_FUZZY = 1;

    public final String name;

    public Lexicon(String name) {
        this.name = name;
    }

    public abstract int getItemCount();

    public abstract String getItemText(int id);

    public abstract Collection<Grammar.ItemParam> getItemParams(int id);

    public abstract int findItem(String text);

    public static final class SearchResult {
        public final int pos, length;
        public int item;
        public double score, innerScore;

        public SearchResult(int pos, int length) {
            this.pos = pos;
            this.length = length;
        }
    }

    public abstract Collection<SearchResult> search(String text);
}
