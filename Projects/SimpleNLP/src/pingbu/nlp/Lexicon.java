package pingbu.nlp;

import java.util.Collection;

/**
 * 词典基类，供语法搜索时引用
 * 
 * @author pingbu
 */
public abstract class Lexicon {

    private static short sPrevId = 0;

    public final String id;

    public Lexicon() {
        this(null);
    }

    public Lexicon(String debugName) {
        if (debugName != null) {
            id = debugName;
        } else {
            synchronized (Lexicon.class) {
                id = Integer.toString(++sPrevId);
            }
        }
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
