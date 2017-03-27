package pingbu.nlp;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pingbu.common.Pinyin;


public class Lexicon {

    private static final boolean LOG = false;
    private static final boolean LOG_TIME = false;

    private static void log(String fmt, Object... args) {
        if (LOG) {
            if (LOG_TIME)
                System.out.printf("[%d] ", System.currentTimeMillis());
            System.out.printf(fmt + "\n", args);
        }
    }

    private static class MatchedItem {
        public int item;
        public char c1, c2;
    }

    private static short sPrevId = 0;

    private short mId;
    private String mDebugName;
    private List<String> mItems = new ArrayList<String>();
    private Map<Short, List<MatchedItem>> mCharIndex = new HashMap<Short, List<MatchedItem>>();
    private Map<String, List<MatchedItem>> mIndex = new HashMap<String, List<MatchedItem>>();

    private static String normalize(char a, char b) {
        return Pinyin.normailizeChar(a) + "-" + Pinyin.normailizeChar(b);
    }

    public Lexicon() {
        mId = ++sPrevId;
        mDebugName = "lex-" + mId;
    }

    public short getId() {
        return mId;
    }

    public String getDebugName() {
        return mDebugName;
    }

    public static Lexicon load(String name) {
        Lexicon lexicon = new Lexicon();
        lexicon.mDebugName = name;
        try {
            FileInputStream f = new FileInputStream(name);
            InputStreamReader in = new InputStreamReader(f, "UTF-8");
            BufferedReader r = new BufferedReader(in);
            for (;;) {
                String l = r.readLine();
                if (l == null)
                    break;
                lexicon.addItem(l);
            }
            r.close();
            in.close();
            f.close();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
        return lexicon;
    }

    public void addItem(String text) {
        mItems.add(text);
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
                List<MatchedItem> index = mIndex.getOrDefault(word, null);
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

    public int getItemCount() {
        return mItems.size();
    }

    public String getItem(int item) {
        return mItems.get(item);
    }

    private static class SearchingIndex {
        public int pos;
        public List<MatchedItem> index;
    }

    private static class MatchedWord {
        public int pos;
        public double scoreL, scoreR, score;
    }

    public static class SearchResult {
        public int pos, length, item;
        public double innerScore, priority;
    }

    public Collection<SearchResult> search(String text) {
        log("Searching for %s in lexicon %s", text, mDebugName);
        Map<Integer, SearchResult> results = new HashMap<Integer, SearchResult>();
        for (int i = 0; i < text.length(); ++i) {
            SearchResult r = null;
            char c = text.charAt(i);
            short nc = Pinyin.normailizeChar(c);
            List<MatchedItem> index = mCharIndex.get(nc);
            if (index != null)
                for (MatchedItem mi : index) {
                    double priority = Pinyin.compareChar(c, mi.c1);
                    if (r == null || priority > r.priority) {
                        if (r == null) {
                            r = new SearchResult();
                            results.put((i << 16) | 1, r);
                        }
                        r.item = mi.item;
                        r.pos = i;
                        r.length = 1;
                        r.innerScore = 0;
                        r.priority = priority;
                    }
                }
        }
        List<SearchingIndex> wordIndexes = new ArrayList<SearchingIndex>();
        for (int i = 1; i < text.length(); ++i) {
            String word = normalize(text.charAt(i - 1), text.charAt(i));
            SearchingIndex index = new SearchingIndex();
            index.pos = i;
            index.index = mIndex.getOrDefault(word, null);
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
                while (index.index != null && wordIndexPos[i] < index.index.size()) {
                    MatchedItem mi = index.index.get(wordIndexPos[i]);
                    if (mi.item > item)
                        break;
                    MatchedWord w = new MatchedWord();
                    w.pos = index.pos;
                    w.scoreL = Pinyin.compareChar(mi.c1, text.charAt(index.pos - 1));
                    w.scoreR = Pinyin.compareChar(mi.c2, text.charAt(index.pos));
                    w.score = w.scoreL * w.scoreR;
                    matchedWords.add(w);
                    ++wordIndexPos[i];
                }
            }
            matchedWords.sort(new Comparator<MatchedWord>() {
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
                    double priority = (innerScore + matchedWords.get(i).scoreL * matchedWords.get(j).scoreR)
                            / Math.max(length, mItems.get(item).length());
                    if (priority >= 0.6) {
                        int resultKey = (pos << 16) | length;
                        SearchResult r = results.get(resultKey);
                        if (r == null || priority > r.priority) {
                            if (r == null) {
                                r = new SearchResult();
                                results.put(resultKey, r);
                            }
                            r.item = item;
                            r.pos = pos;
                            r.length = length;
                            r.innerScore = innerScore;
                            r.priority = priority;
                        }
                    }
                }
            }
        }
        for (SearchResult r : results.values())
            log("%f %f - %s(%d,%d)", r.priority, r.innerScore, getItem(r.item), r.pos, r.length);
        return results.values();
    }
}
