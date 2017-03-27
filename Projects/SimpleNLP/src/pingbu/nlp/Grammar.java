package pingbu.nlp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Grammar {

    private static final boolean LOG = true;
    private static final boolean LOG_TIME = false;
    private static final boolean LOG_PATH_SEARCH = false;

    private static void log(String fmt, Object... args) {
        if (LOG) {
            if (LOG_TIME)
                System.out.printf("[%d] ", System.currentTimeMillis());
            System.out.printf(fmt + "\n", args);
        }
    }

    @SuppressWarnings("unused")
    private static void log(Item item) {
        log("==== Item %s ====", item.commandId);
        log("PATH: %s", pathToString(item.path));
        for (Grammar.ItemParam param : item.params)
            log("  <%s>: %d, %d", param.name, param.pos, param.length);
    }

    public static class ItemParam {
        public String name;
        public int pos, length;
    }

    private static String pathToId(Unit[] path) {
        List<String> ids = new ArrayList<String>();
        for (Unit unit : path)
            ids.add(unit.getId());
        return String.join("-", ids.toArray(new String[ids.size()]));
    }

    private static String pathToString(Unit[] path) {
        StringBuilder sb = new StringBuilder();
        for (Unit unit : path)
            sb.append(unit.toString());
        return sb.toString();
    }

    public static String pathToString(Iterable<?> path) {
        StringBuilder sb = new StringBuilder();
        for (Object unit : path)
            sb.append(unit.toString());
        return sb.toString();
    }

    private static class MatchedItem {
        public int item;
        public Unit unit1, unit2;
    }

    private static class Item {
        public String commandId;
        public Unit[] path;
        public Grammar.ItemParam[] params;
    }

    private static class Index {
        private List<Item> mItems = new ArrayList<Item>();
        private Map<String, List<MatchedItem>> mIndex = new HashMap<String, List<MatchedItem>>();
    }

    private List<Lexicon> mLexicons = new ArrayList<Lexicon>();
    private Set<String> mPaths = new HashSet<String>();
    private Map<String, Index> mSlotPathIndex = new HashMap<String, Index>();

    public void addLexicon(String name, Lexicon lexicon) {
        mLexicons.add(lexicon);
    }

    public void addPath(String commandId, Unit[] path, Grammar.ItemParam[] params) {
        String desc = pathToId(path);
        if (!mPaths.contains(desc)) {
            mPaths.add(desc);
            List<String> lexiconIds = new ArrayList<String>();
            for (int i = 0; i < path.length; ++i) {
                Unit unit = path[i];
                if (unit instanceof UnitLexiconSlot)
                    lexiconIds.add(Integer.toString(((UnitLexiconSlot) unit).getLexiconId()));
            }
            String slotPath = String.join("-", lexiconIds);
            Index slotPathIndex = mSlotPathIndex.get(slotPath);
            if (slotPathIndex == null) {
                slotPathIndex = new Index();
                mSlotPathIndex.put(slotPath, slotPathIndex);
            }
            Item item = new Item();
            item.commandId = commandId;
            item.path = path;
            item.params = params;
            slotPathIndex.mItems.add(item);
            if (path.length == 1) {
                String word = path[0].getId();
                List<MatchedItem> index = slotPathIndex.mIndex.get(word);
                if (index == null) {
                    index = new ArrayList<MatchedItem>();
                    slotPathIndex.mIndex.put(word, index);
                }
                MatchedItem mi = new MatchedItem();
                mi.item = slotPathIndex.mItems.size() - 1;
                mi.unit1 = path[0];
                index.add(mi);
            } else
                for (int i = 1; i < path.length; ++i) {
                    String word = path[i - 1].getId() + "-" + path[i].getId();
                    List<MatchedItem> index = slotPathIndex.mIndex.get(word);
                    if (index == null) {
                        index = new ArrayList<MatchedItem>();
                        slotPathIndex.mIndex.put(word, index);
                    }
                    MatchedItem mi = new MatchedItem();
                    mi.item = slotPathIndex.mItems.size() - 1;
                    mi.unit1 = path[i - 1];
                    mi.unit2 = path[i];
                    index.add(mi);
                }
        }
    }

    private static class SearchingIndex {
        public List<MatchedItem> index;
        public int pos;
    }

    private static class SearchCandidate {
        public String slotPath;
        public int item;
        public Map<Integer, String> units;
        public double priority;
    }

    public static class SearchResult {
        public String commandId;
        public Map<String, String> params;
    }

    private SearchCandidate search(String slotPath, List<Unit.Result> path, Map<String, String> slots,
            int sourceLength, int targetExtLength) {
        if (LOG_PATH_SEARCH)
            log("  Searching for %s:", pathToString(path));
        SearchCandidate result = null;
        Index slotPathIndex = mSlotPathIndex.get(slotPath);
        for (int i = 0; i < path.size(); ++i) {
            Unit.Result c = path.get(i);
            String nc = c.getId();
            List<MatchedItem> index = slotPathIndex.mIndex.get(nc);
            if (index != null)
                for (MatchedItem mi : index) {
                    double priority = (c.compare(mi.unit1) + c.getInnerScore()) / sourceLength;
                    if (result == null || priority > result.priority) {
                        if (result == null) {
                            result = new SearchCandidate();
                            result.slotPath = slotPath;
                        }
                        result.item = mi.item;
                        result.priority = priority;
                        result.units = new HashMap<Integer, String>();
                        Item itemObject = slotPathIndex.mItems.get(mi.item);
                        Unit unit = itemObject.path[0];
                        if (unit instanceof UnitLexiconSlot)
                            result.units.put(0, slots.get(unit.getId()));
                        else
                            result.units.put(0, unit.toString());
                    }
                }
        }
        // 分词并加载索引
        List<SearchingIndex> wordIndexes = new ArrayList<SearchingIndex>();
        for (int i = 1; i < path.size(); ++i) {
            String word = path.get(i - 1).getId() + "-" + path.get(i).getId();
            List<MatchedItem> index = slotPathIndex.mIndex.get(word);
            if (index != null) {
                SearchingIndex wordIndex = new SearchingIndex();
                wordIndex.index = index;
                wordIndex.pos = i;
                wordIndexes.add(wordIndex);
            }
        }
        int[] wordIndexPos = new int[wordIndexes.size()];
        for (;;) {
            // 查找下一条匹配的语法
            int item = Integer.MAX_VALUE;
            for (int i = 0; i < wordIndexes.size(); ++i) {
                List<MatchedItem> index = wordIndexes.get(i).index;
                if (wordIndexPos[i] < index.size()) {
                    int t = index.get(wordIndexPos[i]).item;
                    if (t < item)
                        item = t;
                }
            }
            if (item >= Integer.MAX_VALUE)
                break;
            // 计算该条匹配语法分值
            Set<Integer> innerScoreUnits = new HashSet<Integer>();
            double score = 0;
            for (int i = 0; i < wordIndexes.size(); ++i) {
                SearchingIndex index = wordIndexes.get(i);
                while (wordIndexPos[i] < index.index.size()) {
                    MatchedItem mi = index.index.get(wordIndexPos[i]);
                    if (mi.item > item)
                        break;
                    double score0 = path.get(index.pos - 1).compare(mi.unit1);
                    double score1 = path.get(index.pos).compare(mi.unit2);
                    score += score0 * score1;
                    if (!innerScoreUnits.contains(index.pos - 1)) {
                        innerScoreUnits.add(index.pos - 1);
                        score += path.get(index.pos - 1).getInnerScore();
                    }
                    if (!innerScoreUnits.contains(index.pos)) {
                        innerScoreUnits.add(index.pos);
                        score += path.get(index.pos).getInnerScore();
                    }
                    ++wordIndexPos[i];
                }
            }
            Item itemObject = slotPathIndex.mItems.get(item);
            double priority = score
                    / (Math.max(sourceLength, itemObject.path.length + targetExtLength) - 1);
            if (result == null || priority > result.priority) {
                if (result == null) {
                    result = new SearchCandidate();
                    result.slotPath = slotPath;
                }
                result.item = item;
                result.priority = priority;
                result.units = new HashMap<Integer, String>();
                for (int i = 0; i < itemObject.path.length; ++i) {
                    Unit unit = itemObject.path[i];
                    if (unit instanceof UnitLexiconSlot)
                        result.units.put(i, slots.get(unit.getId()));
                    else
                        result.units.put(i, unit.toString());
                }
            }
        }
        if (LOG_PATH_SEARCH)
            if (result != null)
                log("    %f: %d - %s", result.priority, result.item,
                        pathToString(slotPathIndex.mItems.get(result.item).path));
            else
                log("    no result");
        return result;
    }

    private class SearchContext {
        private Lexicon.SearchResult[][] lexiconResults = new Lexicon.SearchResult[mLexicons.size()][];
        private String mText;

        public SearchContext(String text) {
            mText = text;
        }

        public LexiconSearchProc newLexiconSearchProc(int lexiconIndex) {
            return new LexiconSearchProc(lexiconIndex);
        }

        public PathSearchProc newPathSearchProc(String slotPath, List<Unit.Result> path,
                Map<String, String> slots, int sourceLength, int targetExtLength) {
            return new PathSearchProc(slotPath, path, slots, sourceLength, targetExtLength);
        }

        public class LexiconSearchProc implements Runnable {
            private int mLexiconIndex;

            public Collection<Lexicon.SearchResult> results;

            public LexiconSearchProc(int lexiconIndex) {
                mLexiconIndex = lexiconIndex;
            }

            @Override
            public void run() {
                results = mLexicons.get(mLexiconIndex).search(mText);
            }
        }

        public class PathSearchProc implements Runnable {
            private String slotPath;
            private List<Unit.Result> path;
            private Map<String, String> slots;
            private int sourceLength, targetExtLength;
            public SearchCandidate result;

            public PathSearchProc(String slotPath, List<Unit.Result> path, Map<String, String> slots,
                    int sourceLength, int targetExtLength) {
                this.slotPath = slotPath;
                this.path = path;
                this.slots = slots;
                this.sourceLength = sourceLength;
                this.targetExtLength = targetExtLength;
            }

            @Override
            public void run() {
                result = search(slotPath, path, slots, sourceLength, targetExtLength);
            }
        }
    }

    public SearchResult search(String text) {
        log("*** Searching for %s:", text);
        long t = System.currentTimeMillis();
        SearchCandidate result = null;
        if (mLexicons != null && !mLexicons.isEmpty()) {
            SearchContext searchContext = new SearchContext(text);
            // 先搜索各个词典
            int lexicons = 0;
            SearchContext.LexiconSearchProc[] lexiconSearchProcs = new SearchContext.LexiconSearchProc[mLexicons
                    .size()];
            Thread[] lexiconSearchThreads = new Thread[mLexicons.size()];
            for (int i = 0; i < mLexicons.size(); ++i) {
                lexiconSearchProcs[i] = searchContext.newLexiconSearchProc(i);
                lexiconSearchThreads[i] = new Thread(lexiconSearchProcs[i]);
                lexiconSearchThreads[i].start();
            }
            for (int i = 0; i < mLexicons.size(); ++i) {
                try {
                    lexiconSearchThreads[i].join();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
                Collection<Lexicon.SearchResult> rs = lexiconSearchProcs[i].results;
                if (!rs.isEmpty()) {
                    searchContext.lexiconResults[i] = rs.toArray(new Lexicon.SearchResult[rs.size()]);
                    ++lexicons;
                }
            }
            log(" %d lexicons search finish", lexicons);
            // 对每一组不冲突的可能参数取值执行参数化后的搜索
            List<SearchContext.PathSearchProc> pathSearchProcs = new ArrayList<SearchContext.PathSearchProc>();
            int[] lexiconResultPos = new int[mLexicons.size()];
            for (int i = 0; i < mLexicons.size(); ++i)
                lexiconResultPos[i] = -1;
            L1: for (;;) {
                L2: for (;;) {
                    Unit.Result[] units = new Unit.Result[text.length()];
                    for (int i = 0; i < lexiconResultPos.length; ++i)
                        if (lexiconResultPos[i] >= 0) {
                            Lexicon.SearchResult r = searchContext.lexiconResults[i][lexiconResultPos[i]];
                            for (int j = i + 1; j < lexiconResultPos.length; ++j)
                                if (lexiconResultPos[j] >= 0) {
                                    Lexicon.SearchResult r1 = searchContext.lexiconResults[j][lexiconResultPos[j]];
                                    if (r.pos < r1.pos + r1.length && r1.pos < r.pos + r.length)
                                        break L2;
                                }
                            units[r.pos] = new UnitLexiconSlotResult(mLexicons.get(i), r);
                            for (int j = 1; j < r.length; ++j)
                                units[r.pos + j] = null;
                        }
                    List<String> lexiconIds = new ArrayList<String>();
                    for (int i = 0; i < text.length(); ++i)
                        if (units[i] != null)
                            lexiconIds.add(Integer.toString(((UnitLexiconSlotResult) units[i])
                                    .getLexiconId()));
                    String slotPath = String.join("-", lexiconIds);
                    if (mSlotPathIndex.containsKey(slotPath)) {
                        for (int i = 0; i < text.length(); ++i)
                            units[i] = new UnitCharResult(i, text.charAt(i));
                        for (int i = 0; i < lexiconResultPos.length; ++i) {
                            if (lexiconResultPos[i] >= 0) {
                                Lexicon.SearchResult r = searchContext.lexiconResults[i][lexiconResultPos[i]];
                                units[r.pos] = new UnitLexiconSlotResult(mLexicons.get(i), r);
                                for (int j = 1; j < r.length; ++j)
                                    units[r.pos + j] = null;
                            }
                        }
                        List<Unit.Result> path = new ArrayList<Unit.Result>();
                        Map<String, String> slots = new HashMap<String, String>();
                        int targetExtLength = 0;
                        for (Unit.Result unit : units)
                            if (unit != null) {
                                path.add(unit);
                                if (unit instanceof UnitLexiconSlotResult) {
                                    String slotText = unit.getText();
                                    slots.put(unit.getId(), slotText);
                                    targetExtLength += slotText.length() - 1;
                                }
                            }
                        int sourceLength = path.size() + targetExtLength;
                        pathSearchProcs.add(searchContext.newPathSearchProc(slotPath, path, slots,
                                sourceLength, targetExtLength));
                    }
                    break;
                }
                for (int i = 0;; ++i) {
                    if (i >= lexiconResultPos.length)
                        break L1;
                    if (searchContext.lexiconResults[i] != null) {
                        ++lexiconResultPos[i];
                        if (lexiconResultPos[i] < searchContext.lexiconResults[i].length)
                            break;
                        lexiconResultPos[i] = -1;
                    }
                }
            }
            Thread[] pathSearchThreads = new Thread[pathSearchProcs.size()];
            for (int i = 0; i < pathSearchThreads.length; ++i) {
                pathSearchThreads[i] = new Thread(pathSearchProcs.get(i));
                pathSearchThreads[i].start();
            }
            for (int i = 0; i < pathSearchThreads.length; ++i) {
                try {
                    pathSearchThreads[i].join();
                } catch (Exception e) {
                    e.printStackTrace();
                    System.exit(-1);
                }
                SearchCandidate r = pathSearchProcs.get(i).result;
                if (r != null && r.priority >= 0.6 && (result == null || r.priority > result.priority))
                    result = r;
            }
            log(" %d paths search finish", pathSearchProcs.size());
        }
        t = System.currentTimeMillis() - t;
        log(" %.3fs time used", t / 1000.);
        SearchResult rr = null;
        if (result != null) {
            Item item = mSlotPathIndex.get(result.slotPath).mItems.get(result.item);
            // log(item);
            // for (Map.Entry<Integer, String> unit : result.units.entrySet())
            // log(" Unit[%d]: %s", unit.getKey(), unit.getValue());
            rr = new SearchResult();
            rr.commandId = item.commandId;
            rr.params = new HashMap<String, String>();
            for (Grammar.ItemParam slotInfo : item.params) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < slotInfo.length; ++i) {
                    String unitText;
                    Unit unit = item.path[slotInfo.pos + i];
                    if (unit instanceof UnitLexiconSlot)
                        unitText = result.units.get(slotInfo.pos + i);
                    else
                        unitText = unit.toString();
                    if (unitText != null)
                        sb.append(unitText);
                }
                if (sb.length() > 0)
                    rr.params.put(slotInfo.name, sb.toString());
            }
            log("RESULT: %f - %s", result.priority, rr.commandId);
            for (Map.Entry<String, String> param : rr.params.entrySet())
                log("  <%s>=%s", param.getKey(), param.getValue());
        } else {
            log("NO RESULT!");
        }
        return rr;
    }
}
