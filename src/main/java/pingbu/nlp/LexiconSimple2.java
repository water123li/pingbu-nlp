package pingbu.nlp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pingbu.logger.Logger;
import pingbu.pinyin.Pinyin;

/**
 * 模糊度更高，但效率较低，限制用在用户词典较为合适！
 * 
 * @author pingbu
 */
class LexiconSimple2 extends ListLexicon {
    private static final String TAG = LexiconSimple2.class.getSimpleName();

    private static final boolean LOG = false;
    private static final boolean LOG_RESULT = false;
    private static final double THRESHOLD = .5;

    private static void log(final String fmt, final Object... args) {
        if (LOG)
            Logger.d(TAG, fmt, args);
    }

    private static void log_result(final String fmt, final Object... args) {
        if (LOG_RESULT)
            Logger.d(TAG, fmt, args);
    }

    private static final class Item {
        final String text;
        final ArrayList<Grammar.ItemParam> params;

        Item(final String text, final String params) {
            this.text = text;
            this.params = _parseParams(params);
        }
    }

    private static final class IndexItem {
        final int item, wordPos;
        final char wordChar;

        IndexItem(final int item, final int pos, final char c) {
            this.item = item;
            this.wordPos = pos;
            this.wordChar = c;
        }
    }

    private int mMaxWordLength = 0;
    private final List<Item> mItems = new ArrayList<>();
    private final Map<String, Integer> mItemIndex = new HashMap<>();
    private final Map<Integer, List<IndexItem>> mSMIndex = new HashMap<>();
    private final Map<Integer, List<IndexItem>> mYMIndex = new HashMap<>();

    private static ArrayList<Grammar.ItemParam> _parseParams(final String desc) {
        final ArrayList<Grammar.ItemParam> params = new ArrayList<>();
        if (desc != null)
            for (String item : desc.split(",")) {
                final Grammar.ItemParam param = new Grammar.ItemParam();
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

    public LexiconSimple2(final String name) {
        super(name);
    }

    public LexiconSimple2(final String name, String[] items) {
        this(name);
        addItems(items);
    }

    public LexiconSimple2(final String name, final Iterable<String> items) {
        this(name);
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

    private List<IndexItem> _getIndex(final Map<Integer, List<IndexItem>> index, final int word) {
        List<IndexItem> r = index.get(word);
        if (r == null) {
            r = new ArrayList<>();
            index.put(word, r);
        }
        return r;
    }

    private void _addToIndex(final Map<Integer, List<IndexItem>> index, final int word, final IndexItem indexItem) {
        _getIndex(index, word).add(indexItem);
    }

    @Override
    public final void addItem(final String text, final String params) {
        mItemIndex.put(text, mItems.size());
        mMaxWordLength = Math.max(mMaxWordLength, text.length());
        mItems.add(new Item(text, params));
        for (int pos = 0; pos < text.length(); ++pos) {
            final char c = text.charAt(pos);
            IndexItem ii = new IndexItem(mItems.size() - 1, pos, c);
            final short nc = Pinyin.normailizeChar(c);
            _addToIndex(mSMIndex, Pinyin.getSM(nc), ii);
            _addToIndex(mYMIndex, Pinyin.getYM(nc), ii);
        }
    }

    @Override
    public final int getType() {
        return Lexicon.TYPE_NORMAL;
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

    private static final class SearchingIndex {
        final int textPos;
        final List<IndexItem> index;

        SearchingIndex(final int textPos, final List<IndexItem> index) {
            this.textPos = textPos;
            this.index = index;
        }
    }

    private static final class MatchedWord {
        final double scoreL, scoreR, score;

        MatchedWord(final double scoreL, final double scoreR) {
            this.scoreL = scoreL;
            this.scoreR = scoreR;
            this.score = scoreL * scoreR;
        }
    }

    public final Collection<SearchResult> search(final String text) {
        log_result("Searching for %s in lexicon %s", text, name);
        final Map<Integer, SearchResult> results = new HashMap<>();

        final int textLength = text.length();

        final List<SearchingIndex> wordIndexes = new ArrayList<>();
        for (int pos = 0; pos < textLength; ++pos) {
            final short nc = Pinyin.normailizeChar(text.charAt(pos));
            wordIndexes.add(new SearchingIndex(pos, _getIndex(mSMIndex, Pinyin.getSM(nc))));
            wordIndexes.add(new SearchingIndex(pos, _getIndex(mYMIndex, Pinyin.getYM(nc))));
        }
        wordIndexes.remove(null);

        final Map<Integer, Double> matchedChars = new HashMap<>();
        final MatchedWord[] matchedWords = new MatchedWord[textLength - 1];
        final int[] wordIndexPos = new int[wordIndexes.size()];
        for (;;) {
            // 找到下一条索引条目
            int item = Integer.MAX_VALUE;
            for (int i = 0; i < wordIndexes.size(); ++i) {
                final List<IndexItem> index = wordIndexes.get(i).index;
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
                while (index.index != null && wordIndexPos[i] < index.index.size()) {
                    final IndexItem mi = index.index.get(wordIndexPos[i]);
                    if (mi.item > item)
                        break;
                    final double score = Pinyin.compareChar(mi.wordChar, text.charAt(index.textPos));
                    log("  char %d,%c ~ %d,%c = %.2f", mi.wordPos, mi.wordChar, index.textPos, text.charAt(index.textPos), score);
                    matchedChars.put(index.textPos * wordLength + mi.wordPos, score);
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
                            final int resultKey = (matchedTextPos << 16) | matchedTextLength;
                            SearchResult r = results.get(resultKey);
                            if (r == null || score > r.score) {
                                if (r == null) {
                                    r = new SearchResult(matchedTextPos, matchedTextLength);
                                    results.put(resultKey, r);
                                }
                                r.item = item;
                                r.score = score;
                                r.innerScore = 0;
                                log(" <-- %d,%d %s %.3f", matchedTextPos, matchedTextLength, word.text, score);
                            }
                        }
                    }
                }
            } else {
                // 找到所有本条索引到的词并计算词的匹配分
                for (int textPos = 0; textPos < matchedWords.length; ++textPos) {
                    matchedWords[textPos] = null;
                    for (int wordPos = 0; wordPos < wordLength - 1; ++wordPos) {
                        final Double mcL = matchedChars.get(textPos * wordLength + wordPos);
                        final Double mcR = matchedChars.get((textPos + 1) * wordLength + wordPos + 1);
                        if (mcL != null && mcR != null) {
                            final MatchedWord matchedWord = new MatchedWord(mcL, mcR);
                            matchedWords[textPos] = matchedWord;
                            log("  word %s ~ %d,%c%c = %.3f", word.text.substring(wordPos, wordPos + 2),
                                    textPos, text.charAt(textPos), text.charAt(textPos + 1), matchedWord.score);
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
                        final int matchedTextLength = endTextPos + 2 - beginTextPos;
                        double score = innerScore;
                        if (endTextPos == beginTextPos)
                            score += innerScore;
                        else
                            score += matchedWords[beginTextPos].scoreL * matchedWords[endTextPos].scoreR;
                        score /= Math.max(matchedTextLength, wordLength);
                        if (score >= THRESHOLD) {
                            final int resultKey = (beginTextPos << 16) | matchedTextLength;
                            SearchResult r = results.get(resultKey);
                            if (r == null || score > r.score) {
                                if (r == null) {
                                    r = new SearchResult(beginTextPos, matchedTextLength);
                                    results.put(resultKey, r);
                                }
                                r.item = item;
                                r.score = score;
                                r.innerScore = innerScore;
                                log(" <-- %d,%d %s %.3f", beginTextPos, matchedTextLength, word.text, score);
                            }
                        }
                    }
                }
            }
        }
        for (SearchResult r : results.values())
            log_result("%f %f - %s(%d,%d)", r.score, r.innerScore, getItemText(r.item), r.pos, r.length);
        return results.values();
    }
}
