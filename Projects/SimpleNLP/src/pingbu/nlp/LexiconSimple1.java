package pingbu.nlp;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pingbu.common.MyLog;
import pingbu.common.Pinyin;

public class LexiconSimple1 extends LexiconSimple {
    private static final String TAG = LexiconSimple1.class.getSimpleName();

    private static final boolean LOG = false;
    private static final double THRESHOLD = .6;

    private static final void log(String fmt, Object... args) {
        if (LOG)
            MyLog.logD(TAG, String.format(fmt, args));
    }

    public static LexiconSimple1 load(String path) throws IOException {
        return load(null, path);
    }

    public static LexiconSimple1 load(String debugName, String path)
            throws IOException {
        try (FileInputStream f = new FileInputStream(path)) {
            return load(debugName, f);
        }
    }

    public static LexiconSimple1 load(InputStream f) throws IOException {
        return load(null, f);
    }

    public static LexiconSimple1 load(String debugName, InputStream f)
            throws IOException {
        final LexiconSimple1 lexicon = new LexiconSimple1(debugName);
        try (InputStreamReader in = new InputStreamReader(f, "UTF-8");
                BufferedReader r = new BufferedReader(in)) {
            for (;;) {
                String l = r.readLine();
                if (l == null)
                    break;
                lexicon.addItem(l);
            }
        }
        return lexicon;
    }

    private static final class MatchedItem {
        public int item;
        public char c1, c2;
    }

    private static final class Item {
        public String text;
        public ArrayList<Grammar.ItemParam> params;
    }

    private final List<Item> mItems = new ArrayList<Item>();
    private final Map<String, Integer> mItemIndex = new HashMap<String, Integer>();
    private final Map<Short, List<MatchedItem>> mCharIndex = new HashMap<Short, List<MatchedItem>>();
    private final Map<String, List<MatchedItem>> mIndex = new HashMap<String, List<MatchedItem>>();

    private static final String normalize(char a, char b) {
        return Pinyin.normailizeChar(a) + "-" + Pinyin.normailizeChar(b);
    }

    private static ArrayList<Grammar.ItemParam> _parseParams(String desc) {
        ArrayList<Grammar.ItemParam> params = new ArrayList<Grammar.ItemParam>();
        if (desc != null)
            for (String item : desc.split(",")) {
                Grammar.ItemParam param = new Grammar.ItemParam();
                int p = item.indexOf('=');
                if (p > 0) {
                    param.key = item.substring(0, p);
                    param.value = item.substring(p + 1);
                } else {
                    param.key = item;
                    param.value = "";
                }
                params.add(param);
            }
        return params;
    }

    public LexiconSimple1() {
        super();
    }

    public LexiconSimple1(String debugName) {
        super(debugName);
    }

    public LexiconSimple1(String[] items) {
        this();
        addItems(items);
    }

    public LexiconSimple1(Iterable<String> items) {
        this();
        addItems(items);
    }

    public LexiconSimple1(String debugName, String[] items) {
        this(debugName);
        addItems(items);
    }

    public LexiconSimple1(String debugName, Iterable<String> items) {
        this(debugName);
        addItems(items);
    }

    @Override
    public final void addItems(String[] items) {
        for (String item : items)
            addItem(item);
    }

    @Override
    public final void addItems(Iterable<String> items) {
        for (String item : items)
            addItem(item);
    }

    @Override
    public final void addItem(String text) {
        addItem(text, null);
    }

    @Override
    public final void addItem(String text, String params) {
        mItemIndex.put(text, mItems.size());
        Item item = new Item();
        item.text = text;
        item.params = _parseParams(params);
        mItems.add(item);
        if (text.length() == 1) {
            char c = text.charAt(0);
            short nc = Pinyin.normailizeChar(c);
            List<MatchedItem> index = mCharIndex.get(nc);
            if (index == null) {
                index = new ArrayList<MatchedItem>();
                mCharIndex.put(nc, index);
            }
            MatchedItem mi = new MatchedItem();
            mi.item = mItems.size() - 1;
            mi.c1 = c;
            index.add(mi);
        } else
            for (int i = 1; i < text.length(); ++i) {
                String word = normalize(text.charAt(i - 1), text.charAt(i));
                List<MatchedItem> index = mIndex.get(word);
                if (index == null) {
                    index = new ArrayList<MatchedItem>();
                    mIndex.put(word, index);
                }
                MatchedItem mi = new MatchedItem();
                mi.item = mItems.size() - 1;
                mi.c1 = text.charAt(i - 1);
                mi.c2 = text.charAt(i);
                index.add(mi);
            }
    }

    @Override
    public final int getItemCount() {
        return mItems.size();
    }

    @Override
    public final String getItemText(int id) {
        return mItems.get(id).text;
    }

    @Override
    public final Collection<Grammar.ItemParam> getItemParams(int id) {
        return mItems.get(id).params;
    }

    @Override
    public final int findItem(String text) {
        Integer id = mItemIndex.get(text);
        return id == null ? -1 : id;
    }

    private final static class SearchingIndex {
        public int pos;
        public List<MatchedItem> index;
    }

    private final static class MatchedWord {
        public int pos;
        public double scoreL, scoreR, score;
    }

    public final Collection<SearchResult> search(String text) {
        log("Searching for %s in lexicon %s", text, id);
        Map<Integer, SearchResult> results = new HashMap<Integer, SearchResult>();
        for (int i = 0; i < text.length(); ++i) {
            SearchResult r = null;
            char c = text.charAt(i);
            short nc = Pinyin.normailizeChar(c);
            List<MatchedItem> index = mCharIndex.get(nc);
            if (index != null)
                for (MatchedItem mi : index) {
                    double score = Pinyin.compareChar(c, mi.c1);
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
        List<SearchingIndex> wordIndexes = new ArrayList<SearchingIndex>();
        for (int i = 1; i < text.length(); ++i) {
            String word = normalize(text.charAt(i - 1), text.charAt(i));
            SearchingIndex index = new SearchingIndex();
            index.pos = i;
            index.index = mIndex.get(word);
            if (index != null)
                wordIndexes.add(index);
        }
        int[] wordIndexPos = new int[wordIndexes.size()];
        for (;;) {
            int item = Integer.MAX_VALUE;
            for (int i = 0; i < wordIndexes.size(); ++i) {
                List<MatchedItem> index = wordIndexes.get(i).index;
                if (index != null && wordIndexPos[i] < index.size()) {
                    int t = index.get(wordIndexPos[i]).item;
                    if (t < item)
                        item = t;
                }
            }
            if (item >= Integer.MAX_VALUE)
                break;
            List<MatchedWord> matchedWords = new ArrayList<MatchedWord>();
            for (int i = 0; i < wordIndexes.size(); ++i) {
                SearchingIndex index = wordIndexes.get(i);
                while (index.index != null
                        && wordIndexPos[i] < index.index.size()) {
                    MatchedItem mi = index.index.get(wordIndexPos[i]);
                    if (mi.item > item)
                        break;
                    MatchedWord w = new MatchedWord();
                    w.pos = index.pos;
                    w.scoreL = Pinyin.compareChar(mi.c1,
                            text.charAt(index.pos - 1));
                    w.scoreR = Pinyin
                            .compareChar(mi.c2, text.charAt(index.pos));
                    w.score = w.scoreL * w.scoreR;
                    matchedWords.add(w);
                    ++wordIndexPos[i];
                }
            }
            Collections.sort(matchedWords, new Comparator<MatchedWord>() {
                @Override
                public int compare(MatchedWord w1, MatchedWord w2) {
                    return w1.pos - w2.pos;
                }
            });
            for (int i = 0; i < matchedWords.size(); ++i) {
                int pos = matchedWords.get(i).pos - 1;
                for (int j = i; j < matchedWords.size(); ++j) {
                    int length = matchedWords.get(j).pos - pos + 1;
                    double innerScore = 0;
                    for (int k = i; k <= j; ++k)
                        innerScore += matchedWords.get(k).score;
                    double score = (innerScore + matchedWords.get(i).scoreL
                            * matchedWords.get(j).scoreR)
                            / Math.max(length, mItems.get(item).text.length());
                    if (score >= THRESHOLD) {
                        int resultKey = (pos << 16) | length;
                        SearchResult r = results.get(resultKey);
                        if (r == null || score > r.score) {
                            if (r == null) {
                                r = new SearchResult(pos, length);
                                results.put(resultKey, r);
                            }
                            r.item = item;
                            r.score = score;
                            r.innerScore = innerScore;
                        }
                    }
                }
            }
        }
        for (SearchResult r : results.values())
            log("%f %f - %s(%d,%d)", r.score, r.innerScore,
                    getItemText(r.item), r.pos, r.length);
        return results.values();
    }
}
