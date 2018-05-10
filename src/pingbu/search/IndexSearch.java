package pingbu.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pingbu.common.Pinyin;

public class IndexSearch implements Index {

    private static String normalize(final char a, final char b) {
        return Pinyin.normailizeChar(a) + "-" + Pinyin.normailizeChar(b);
    }

    private static class MatchedItem {
        public final int id;
        public final char c1, c2;

        public MatchedItem(final int id, final char c1, final char c2) {
            this.id = id;
            this.c1 = c1;
            this.c2 = c2;
        }
    }

    private static class SearchingIndex {
        public final int pos;
        public final List<MatchedItem> index;

        public SearchingIndex(final int pos, final List<MatchedItem> index) {
            this.pos = pos;
            this.index = index;
        }
    }

    private final Map<Short, List<MatchedItem>> mCharIndex = new HashMap<Short, List<MatchedItem>>();
    private final Map<String, List<MatchedItem>> mIndex = new HashMap<String, List<MatchedItem>>();

    @Override
    public void addItem(final int id, final String value) {
        for (int i = 0; i < value.length(); ++i) {
            final char c = value.charAt(i);
            final short nc = Pinyin.normailizeChar(c);
            List<MatchedItem> index = mCharIndex.get(nc);
            if (index == null) {
                index = new ArrayList<MatchedItem>();
                mCharIndex.put(nc, index);
            }
            index.add(new MatchedItem(id, c, '\0'));
        }
        for (int i = 1; i < value.length(); ++i) {
            final String word = normalize(value.charAt(i - 1), value.charAt(i));
            List<MatchedItem> index = mIndex.get(word);
            if (index == null) {
                index = new ArrayList<MatchedItem>();
                mIndex.put(word, index);
            }
            index.add(new MatchedItem(id, value.charAt(i - 1), value.charAt(i)));
        }
    }

    @Override
    public Iterator iterate(final String value) {
        return new Iterator(value);
    }

    private final class Iterator implements Index.Iterator {
        private final String mText;
        private final List<SearchingIndex> mWordIndexes = new ArrayList<SearchingIndex>();
        private final int[] mWordIndexPos;

        public Iterator(final String text) {
            mText = text;
            for (int i = 0; i < text.length(); ++i) {
                final char c = text.charAt(i);
                final short nc = Pinyin.normailizeChar(c);
                final SearchingIndex index = new SearchingIndex(i,
                        mCharIndex.get(nc));
                mWordIndexes.add(index);
            }
            for (int i = 1; i < text.length(); ++i) {
                final String word = normalize(text.charAt(i - 1),
                        text.charAt(i));
                final SearchingIndex index = new SearchingIndex(i,
                        mIndex.get(word));
                mWordIndexes.add(index);
            }
            mWordIndexPos = new int[mWordIndexes.size()];
        }

        @Override
        public int getNextItem() {
            int id = Integer.MAX_VALUE;
            for (int i = 0; i < mWordIndexes.size(); ++i) {
                final List<MatchedItem> index = mWordIndexes.get(i).index;
                if (index != null && mWordIndexPos[i] < index.size()) {
                    final int t = index.get(mWordIndexPos[i]).id;
                    if (t < id)
                        id = t;
                }
            }
            return id;
        }

        @Override
        public double sumupToItem(final int id) {
            double score = 0;
            for (int i = 0; i < mWordIndexes.size(); ++i) {
                final SearchingIndex index = mWordIndexes.get(i);
                while (index.index != null
                        && mWordIndexPos[i] < index.index.size()) {
                    final MatchedItem mi = index.index.get(mWordIndexPos[i]);
                    if (mi.id > id)
                        break;
                    if (mi.c2 == 0)
                        score += Pinyin.compareChar(mi.c1,
                                mText.charAt(index.pos));
                    else
                        score += Pinyin.compareChar(mi.c1,
                                mText.charAt(index.pos - 1))
                                * Pinyin.compareChar(mi.c2,
                                        mText.charAt(index.pos)) * 4;
                    ++mWordIndexPos[i];
                }
            }
            score /= mText.length() * 5 - 4;
            return Math.min(score, 1);
        }

        @Override
        public void close() throws Exception {
        }
    }
}
