package pingbu.nlp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pingbu.logger.Logger;
import pingbu.pinyin.Pinyin;

class LexiconSimple1 extends ListLexicon {
    private static final String TAG = LexiconSimple1.class.getSimpleName();

    private static final boolean LOG = false;
    private static final double THRESHOLD = .6;

    private static void log(final String fmt, final Object... args) {
        if (LOG)
            Logger.d(TAG, fmt, args);
    }

    private static String normalize(final char a, final char b) {
        return Pinyin.normailizeChar(a) + "-" + Pinyin.normailizeChar(b);
    }

    private static final class Item {
        final String text;
        public final ArrayList<Grammar.ItemParam> params = new ArrayList<>();

        public Item(final String text, final String params) {
            this.text = text;
            if (params != null)
                for (final String item : params.split(",")) {
                    final Grammar.ItemParam param = new Grammar.ItemParam();
                    int p = item.indexOf('=');
                    if (p > 0) {
                        param.key = item.substring(0, p);
                        param.value = item.substring(p + 1);
                    } else {
                        param.key = item;
                        param.value = "";
                    }
                    this.params.add(param);
                }
        }
    }

    private static final class MatchedItem {
        final int item;
        final char c1, c2;

        MatchedItem(final int item, final char c1, final char c2) {
            this.item = item;
            this.c1 = c1;
            this.c2 = c2;
        }
    }

    private final static class SearchingIndex {
        final int pos;
        final List<MatchedItem> index;

        SearchingIndex(final int pos, final List<MatchedItem> index) {
            this.pos = pos;
            this.index = index;
        }
    }

    private final static class MatchedWord {
        final int pos;
        final double scoreL, scoreR, score;

        MatchedWord(final int pos, final double scoreL, final double scoreR) {
            this.pos = pos;
            this.scoreL = scoreL;
            this.scoreR = scoreR;
            this.score = scoreL * scoreR;
        }
    }

    private final boolean mFuzzy;
    private final List<Item> mItems = new ArrayList<>();
    private final Map<String, Integer> mItemIndex = new HashMap<>();
    private final Map<Short, List<MatchedItem>> mCharIndex = new HashMap<>();
    private final Map<String, List<MatchedItem>> mIndex = new HashMap<>();

    public LexiconSimple1(final String name, final boolean fuzzy) {
        super(name);
        mFuzzy = fuzzy;
    }

    public LexiconSimple1(final String name, final boolean fuzzy, final String[] items) {
        this(name, fuzzy);
        addItems(items);
    }

    public LexiconSimple1(final String name, final boolean fuzzy, final Iterable<String> items) {
        this(name, fuzzy);
        addItems(items);
    }

    @Override
    public final void addItems(final String[] items) {
        for (final String item : items)
            addItem(item);
    }

    @Override
    public final void addItems(final Iterable<String> items) {
        for (final String item : items)
            addItem(item);
    }

    @Override
    public final void addItem(final String text) {
        addItem(text, null);
    }

    @Override
    public final void addItem(final String text, final String params) {
        mItemIndex.put(text, mItems.size());
        mItems.add(new Item(text, params));
        if (text.length() == 1) {
            final char c = text.charAt(0);
            final short nc = Pinyin.normailizeChar(c);
            List<MatchedItem> index = mCharIndex.get(nc);
            if (index == null) {
                index = new ArrayList<>();
                mCharIndex.put(nc, index);
            }
            index.add(new MatchedItem(mItems.size() - 1, c, '\0'));
        } else
            for (int i = 1; i < text.length(); ++i) {
                final String word = normalize(text.charAt(i - 1), text.charAt(i));
                List<MatchedItem> index = mIndex.get(word);
                if (index == null) {
                    index = new ArrayList<>();
                    mIndex.put(word, index);
                }
                index.add(new MatchedItem(mItems.size() - 1, text.charAt(i - 1), text.charAt(i)));
            }
    }

    @Override
    public final int getType() {
        return mFuzzy ? Lexicon.TYPE_FUZZY : Lexicon.TYPE_NORMAL;
    }

    @Override
    public final int getItemCount() {
        return mItems.size();
    }

    @Override
    public final String getItemText(final int id) {
        return mItems.get(id).text;
    }

    @Override
    public final Collection<Grammar.ItemParam> getItemParams(final int id) {
        return mItems.get(id).params;
    }

    @Override
    public final int findItem(final String text) {
        final Integer id = mItemIndex.get(text);
        return id == null ? -1 : id;
    }

    public final Collection<SearchResult> search(final String text) {
        log("Searching for %s in lexicon %s", text, name);
        final Map<Integer, SearchResult> results = new HashMap<>();

        // 1. Generate single character matches for all position
        for (int i = 0; i < text.length(); ++i) {
            SearchResult r = null;
            final char c = text.charAt(i);
            final short nc = Pinyin.normailizeChar(c);
            final List<MatchedItem> index = mCharIndex.get(nc);
            if (index != null)
                for (final MatchedItem mi : index) {
                    final double score = Pinyin.compareChar(c, mi.c1);
                    if (r == null || score > r.score) {
                        if (r == null) {
                            r = new SearchResult(i, 1);
                            results.put((i << 16) | 1, r);
                        }
                        r.item = mi.item;
                        r.score = score;
                        r.innerScore = 0;
                    }
                }
        }

        // 2. Generate multiply-character matches for all position
        // 2.1 Prepare indexes of all dual-character words
        final List<SearchingIndex> wordIndexes = new ArrayList<SearchingIndex>();
        for (int i = 1; i < text.length(); ++i) {
            final String word = normalize(text.charAt(i - 1), text.charAt(i));
            final SearchingIndex index = new SearchingIndex(i, mIndex.get(word));
            wordIndexes.add(index);
        }

        // 2.2 Iterate all items matches these indexes
        final int[] wordIndexPos = new int[wordIndexes.size()];
        for (;;) {
            // 2.2.1 Find next item matches these indexes
            int item = Integer.MAX_VALUE;
            for (int i = 0; i < wordIndexes.size(); ++i) {
                final List<MatchedItem> index = wordIndexes.get(i).index;
                if (index != null && wordIndexPos[i] < index.size()) {
                    final int t = index.get(wordIndexPos[i]).item;
                    if (t < item)
                        item = t;
                }
            }
            if (item >= Integer.MAX_VALUE)
                break;

            // 2.2.2 Calculate scores for every index of current item
            final List<MatchedWord> matchedWords = new ArrayList<>();
            for (int i = 0; i < wordIndexes.size(); ++i) {
                final SearchingIndex index = wordIndexes.get(i);
                while (index.index != null && wordIndexPos[i] < index.index.size()) {
                    final MatchedItem mi = index.index.get(wordIndexPos[i]);
                    if (mi.item > item)
                        break;
                    final MatchedWord w = new MatchedWord(index.pos,
                            Pinyin.compareChar(mi.c1, text.charAt(index.pos - 1)),
                            Pinyin.compareChar(mi.c2, text.charAt(index.pos)));
                    matchedWords.add(w);
                    ++wordIndexPos[i];
                }
            }

            // 2.2.3 Sum up scores for every continuous section of current item
            Collections.sort(matchedWords, new Comparator<MatchedWord>() {
                @Override
                public int compare(final MatchedWord w1, final MatchedWord w2) {
                    return w1.pos - w2.pos;
                }
            });
            for (int i = 0; i < matchedWords.size(); ++i) {
                final int pos = matchedWords.get(i).pos - 1;
                for (int j = i; j < matchedWords.size(); ++j) {
                    final int length = matchedWords.get(j).pos - pos + 1;
                    double innerScore = 0;
                    for (int k = i; k <= j; ++k)
                        innerScore += matchedWords.get(k).score;
                    final double score = (innerScore + matchedWords.get(i).scoreL * matchedWords.get(j).scoreR)
                            / (mFuzzy ? length : Math.max(length, mItems.get(item).text.length()));
                    if (score >= THRESHOLD) {
                        final int resultKey = (pos << 16) | length;
                        SearchResult r = results.get(resultKey);
                        if (r == null || score > r.score) {
                            if (r == null) {
                                r = new SearchResult(pos, length);
                                results.put(resultKey, r);
                            }
                            r.item = item;
                            r.score = Math.min(score, 1);
                            r.innerScore = Math.min(innerScore, length - 1);
                        }
                    }
                }
            }
        }

        // 3. Output results
        for (final SearchResult r : results.values())
            log("%f %f - %s(%d,%d)", r.score, r.innerScore, getItemText(r.item), r.pos, r.length);
        return results.values();
    }
}
