package pingbu.nlp;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.SparseArray;
import pingbu.common.MyLog;
import pingbu.common.Pinyin;

/**
 * 模糊度更高，但效率较低，限制用在用户词典较为合适！
 * 
 * @author pingbu
 */
public class LexiconSimple2 extends LexiconSimple {
    private static final String TAG = LexiconSimple2.class.getSimpleName();

    private static final boolean LOG = false;
    private static final boolean LOG_RESULT = false;
    private static final double THRESHOLD = .5;

    private static final void log(String fmt, Object... args) {
        if (LOG)
            MyLog.logD(TAG, String.format(fmt, args));
    }

    private static final void log_result(String fmt, Object... args) {
        if (LOG_RESULT)
            MyLog.logD(TAG, String.format(fmt, args));
    }

    public static LexiconSimple2 load(String path) throws IOException {
        return load(null, path);
    }

    public static LexiconSimple2 load(String debugName, String path)
            throws IOException {
        try (FileInputStream f = new FileInputStream(path)) {
            return load(debugName, f);
        }
    }

    public static LexiconSimple2 load(InputStream f) throws IOException {
        return load(null, f);
    }

    public static LexiconSimple2 load(String debugName, InputStream f)
            throws IOException {
        final LexiconSimple2 lexicon = new LexiconSimple2(debugName);
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

    private static final class Item {
        public final String text;
        public final ArrayList<Grammar.ItemParam> params;

        public Item(String text, String params) {
            this.text = text;
            this.params = _parseParams(params);
        }
    }

    private static final class IndexItem {
        public final int item, wordPos;
        public final char wordChar;

        public IndexItem(int item, int pos, char c) {
            this.item = item;
            this.wordPos = pos;
            this.wordChar = c;
        }
    }

    private int mMaxWordLength = 0;
    private final List<Item> mItems = new ArrayList<Item>();
    private final Map<String, Integer> mItemIndex = new HashMap<String, Integer>();
    private final SparseArray<List<IndexItem>> mSMIndex = new SparseArray<List<IndexItem>>();
    private final SparseArray<List<IndexItem>> mYMIndex = new SparseArray<List<IndexItem>>();

    private static final ArrayList<Grammar.ItemParam> _parseParams(String desc) {
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

    public LexiconSimple2(String name) {
        super(name);
    }

    public LexiconSimple2(String name, String[] items) {
        this(name);
        addItems(items);
    }

    public LexiconSimple2(String name, Iterable<String> items) {
        this(name);
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

    private final List<IndexItem> _getIndex(SparseArray<List<IndexItem>> index,
            int word) {
        List<IndexItem> r = index.get(word);
        if (r == null) {
            r = new ArrayList<IndexItem>();
            index.put(word, r);
        }
        return r;
    }

    private final void _addToIndex(SparseArray<List<IndexItem>> index,
            int word, IndexItem indexItem) {
        _getIndex(index, word).add(indexItem);
    }

    @Override
    public final void addItem(String text, String params) {
        mItemIndex.put(text, mItems.size());
        mMaxWordLength = Math.max(mMaxWordLength, text.length());
        mItems.add(new Item(text, params));
        for (int pos = 0; pos < text.length(); ++pos) {
            char c = text.charAt(pos);
            IndexItem ii = new IndexItem(mItems.size() - 1, pos, c);
            short nc = Pinyin.normailizeChar(c);
            _addToIndex(mSMIndex, Pinyin.getSM(nc), ii);
            _addToIndex(mYMIndex, Pinyin.getYM(nc), ii);
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

    private static final class SearchingIndex {
        public final int textPos;
        public final List<IndexItem> index;

        public SearchingIndex(int textPos, List<IndexItem> index) {
            this.textPos = textPos;
            this.index = index;
        }
    }

    private static final class MatchedWord {
        public final double scoreL, scoreR, score;

        public MatchedWord(double scoreL, double scoreR) {
            this.scoreL = scoreL;
            this.scoreR = scoreR;
            this.score = scoreL * scoreR;
        }
    }

    public final Collection<SearchResult> search(String text) {
        log_result("Searching for %s in lexicon %s", text, name);
        Map<Integer, SearchResult> results = new HashMap<Integer, SearchResult>();

        final int textLength = text.length();

        final List<SearchingIndex> wordIndexes = new ArrayList<SearchingIndex>();
        for (int pos = 0; pos < textLength; ++pos) {
            short nc = Pinyin.normailizeChar(text.charAt(pos));
            wordIndexes.add(new SearchingIndex(pos, _getIndex(mSMIndex,
                    Pinyin.getSM(nc))));
            wordIndexes.add(new SearchingIndex(pos, _getIndex(mYMIndex,
                    Pinyin.getYM(nc))));
        }
        wordIndexes.remove(null);

        final SparseArray<Double> matchedChars = new SparseArray<Double>();
        final MatchedWord[] matchedWords = new MatchedWord[textLength - 1];
        final int[] wordIndexPos = new int[wordIndexes.size()];
        for (;;) {
            // 找到下一条索引条目
            int item = Integer.MAX_VALUE;
            for (int i = 0; i < wordIndexes.size(); ++i) {
                List<IndexItem> index = wordIndexes.get(i).index;
                if (index != null && wordIndexPos[i] < index.size()) {
                    int t = index.get(wordIndexPos[i]).item;
                    if (t < item)
                        item = t;
                }
            }
            if (item >= Integer.MAX_VALUE)
                break;
            final Item word = mItems.get(item);
            final int wordLength = word.text.length();
            log(" --> item: %s", word.text);

            // 找到所有本条索引到的字符
            matchedChars.clear();
            for (int i = 0; i < wordIndexes.size(); ++i) {
                final SearchingIndex index = wordIndexes.get(i);
                while (index.index != null
                        && wordIndexPos[i] < index.index.size()) {
                    IndexItem mi = index.index.get(wordIndexPos[i]);
                    if (mi.item > item)
                        break;
                    final double score = Pinyin.compareChar(mi.wordChar,
                            text.charAt(index.textPos));
                    log("  char %d,%c ~ %d,%c = %.2f", mi.wordPos, mi.wordChar,
                            index.textPos, text.charAt(index.textPos), score);
                    matchedChars.put(index.textPos * wordLength + mi.wordPos,
                            score);
                    ++wordIndexPos[i];
                }
            }

            if (wordLength == 1) {
                // 单字词直接根据字符匹配分生成结果
                for (int textPos = 0; textPos < textLength; ++textPos) {
                    final Double matchedChar = matchedChars.get(textPos);
                    if (matchedChar != null) {
                        final double score = matchedChar;
                        if (score >= THRESHOLD) {
                            final int matchedTextPos = textPos;
                            final int matchedTextLength = 1;
                            final int resultKey = (matchedTextPos << 16)
                                    | matchedTextLength;
                            SearchResult r = results.get(resultKey);
                            if (r == null || score > r.score) {
                                if (r == null) {
                                    r = new SearchResult(matchedTextPos,
                                            matchedTextLength);
                                    results.put(resultKey, r);
                                }
                                r.item = item;
                                r.score = score;
                                r.innerScore = 0;
                                log(" <-- %d,%d %s %.3f", matchedTextPos,
                                        matchedTextLength, word.text, score);
                            }
                        }
                    }
                }
            } else {
                // 找到所有本条索引到的词并计算词的匹配分
                for (int textPos = 0; textPos < matchedWords.length; ++textPos) {
                    matchedWords[textPos] = null;
                    for (int wordPos = 0; wordPos < wordLength - 1; ++wordPos) {
                        Double mcL = matchedChars.get(textPos * wordLength
                                + wordPos);
                        Double mcR = matchedChars.get((textPos + 1)
                                * wordLength + wordPos + 1);
                        if (mcL != null && mcR != null) {
                            MatchedWord matchedWord = new MatchedWord(mcL, mcR);
                            matchedWords[textPos] = matchedWord;
                            log("  word %s ~ %d,%c%c = %.3f",
                                    word.text.substring(wordPos, wordPos + 2),
                                    textPos, text.charAt(textPos),
                                    text.charAt(textPos + 1), matchedWord.score);
                        }
                    }
                }

                // 遍历得出所有结果
                for (int beginTextPos = 0; beginTextPos < matchedWords.length; ++beginTextPos) {
                    if (matchedWords[beginTextPos] == null)
                        continue;
                    double innerScore = 0;
                    for (int endTextPos = beginTextPos; endTextPos < matchedWords.length; ++endTextPos) {
                        if (matchedWords[endTextPos] == null)
                            continue;
                        innerScore += matchedWords[endTextPos].score;
                        final int matchedTextLength = endTextPos + 2
                                - beginTextPos;
                        double score = innerScore;
                        if (endTextPos == beginTextPos)
                            score += innerScore;
                        else
                            score += matchedWords[beginTextPos].scoreL
                                    * matchedWords[endTextPos].scoreR;
                        score /= Math.max(matchedTextLength, wordLength);
                        if (score >= THRESHOLD) {
                            final int resultKey = (beginTextPos << 16)
                                    | matchedTextLength;
                            SearchResult r = results.get(resultKey);
                            if (r == null || score > r.score) {
                                if (r == null) {
                                    r = new SearchResult(beginTextPos,
                                            matchedTextLength);
                                    results.put(resultKey, r);
                                }
                                r.item = item;
                                r.score = score;
                                r.innerScore = innerScore;
                                log(" <-- %d,%d %s %.3f", beginTextPos,
                                        matchedTextLength, word.text, score);
                            }
                        }
                    }
                }
            }
        }
        for (SearchResult r : results.values())
            log_result("%f %f - %s(%d,%d)", r.score, r.innerScore,
                    getItemText(r.item), r.pos, r.length);
        return results.values();
    }
}
