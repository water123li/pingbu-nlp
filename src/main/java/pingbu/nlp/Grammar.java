package pingbu.nlp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pingbu.logger.Logger;

/**
 * 语法树模块
 * 
 * @author pingbu
 */
public final class Grammar {
    private static final String TAG = Grammar.class.getSimpleName();

    private static final boolean MT = false;
    private static final boolean LOG = false;
    private static final boolean LOG_RESULT = false;

    private static void log(final String fmt, final Object... args) {
        if (LOG)
            Logger.d(TAG, fmt, args);
    }

    private static void log_result(final String fmt, final Object... args) {
        if (LOG_RESULT)
            Logger.d(TAG, fmt, args);
    }

    protected static final class ItemSlot {
        public String name;
        int pos, length;
    }

    protected static final class ItemParam {
        public String key, value;
    }

    private final Subtree mGrammarTree;
    private final List<Lexicon> mLexicons;

    protected Grammar(final Subtree tree, final Collection<Lexicon> lexicons) {
        mGrammarTree = tree;
        mLexicons = new ArrayList<>(lexicons);
    }

    /**
     * 语法搜索结果
     */
    public static final class SearchResult {
        /**
         * 语法参数
         */
        public final Map<String, String> params;

        /**
         * 语法得分，满分1.0
         */
        public final double score;

        /**
         * 语法搜索时间，单位秒
         */
        public final double time;

        private SearchResult(final Map<String, String> params, final double score, final double time) {
            this.params = params;
            this.score = score;
            this.time = time;
        }
    }

    private final class SearchContext {
        private final long mTime0 = System.currentTimeMillis();
        private final Lexicon.SearchResult[][] mLexiconResults = new Lexicon.SearchResult[mLexicons.size()][];
        private final String mText;
        private final LexiconSearchResultList[] mPosLexiconSearchResults;

        SearchContext(final String text) {
            mText = text;
            mPosLexiconSearchResults = new LexiconSearchResultList[text.length()];
        }

        private final class LexiconSearchProc implements Runnable {
            private final int mLexiconIndex;

            Collection<Lexicon.SearchResult> results;

            LexiconSearchProc(int lexiconIndex) {
                mLexiconIndex = lexiconIndex;
            }

            @Override
            public final void run() {
                results = mLexicons.get(mLexiconIndex).search(mText);
            }
        }

        final void searchLexicons() {
            if (mLexicons != null && !mLexicons.isEmpty()) {
                final long t0 = System.currentTimeMillis();
                int lexicons = 0;
                LexiconSearchProc[] lexiconSearchProcs = new LexiconSearchProc[mLexicons
                        .size()];
                Thread[] lexiconSearchThreads = null;
                if (MT)
                    lexiconSearchThreads = new Thread[mLexicons.size()];
                for (int lexicon = 0; lexicon < mLexicons.size(); ++lexicon) {
                    lexiconSearchProcs[lexicon] = new LexiconSearchProc(lexicon);
                    if (MT) {
                        lexiconSearchThreads[lexicon] = new Thread(lexiconSearchProcs[lexicon]);
                        lexiconSearchThreads[lexicon].start();
                    } else {
                        lexiconSearchProcs[lexicon].run();
                    }
                }
                for (int lexicon = 0; lexicon < mLexicons.size(); ++lexicon) {
                    if (MT)
                        try {
                            lexiconSearchThreads[lexicon].join();
                        } catch (Exception e) {
                            e.printStackTrace();
                            throw new RuntimeException(e);
                        }
                    final Collection<Lexicon.SearchResult> rs = lexiconSearchProcs[lexicon].results;
                    if (!rs.isEmpty()) {
                        mLexiconResults[lexicon] = rs.toArray(new Lexicon.SearchResult[rs.size()]);
                        ++lexicons;
                    }
                }
                final long t = System.currentTimeMillis();
                log_result(" %d lexicons search finish, %.3fs time used", lexicons, (t - t0) / 1000.);
            }
        }

        private void _initSourceMatrix() {
            for (int pos = 0; pos < mPosLexiconSearchResults.length; ++pos) {
                mPosLexiconSearchResults[pos] = new LexiconSearchResultList();
                final LexiconSearchResult lr = new LexiconSearchResult();
                lr.unitResult = new UnitCharResult(mText.charAt(pos));
                lr.length = 1;
                mPosLexiconSearchResults[pos].add(lr);
            }
            for (int lexicon = 0; lexicon < mLexiconResults.length; ++lexicon) {
                final Lexicon.SearchResult[] rs = mLexiconResults[lexicon];
                if (rs != null) {
                    for (Lexicon.SearchResult r : rs) {
                        LexiconSearchResult lr = new LexiconSearchResult();
                        lr.unitResult = new UnitLexiconSlotResult(
                                mLexicons.get(lexicon), mText, r);
                        lr.length = r.length;
                        mPosLexiconSearchResults[r.pos].add(lr);
                    }
                }
            }
        }

        private SearchNode mBestPath = null;
        private int mBestPathDepth = 0;
        private final ArrayList<ItemParam> mBestPathParams = new ArrayList<ItemParam>();
        private final ArrayList<ItemSlot> mBestPathSlots = new ArrayList<ItemSlot>();
        private double mBestPathScore = 0;

        private final class SearchNavigator implements Subtree.Cursor.Navigator {
            private int mDepth = 0;
            private SearchNodes mNodes = new SearchNodes();
            private final ArrayList<Collection<ItemParam>> mParamss = new ArrayList<>();
            private final ArrayList<ItemSlot> mSlots = new ArrayList<>();

            SearchNavigator() {
                for (int pos = 0; pos < mText.length(); ++pos) {
                    final SearchNode node = new SearchNode();
                    node.pos = pos;
                    mNodes.nodes.add(node);
                }
            }

            @Override
            public boolean extendLexicon() {
                return false;
            }

            @Override
            public boolean pushUnit(final Unit unit) {
                final SearchNodes nextNodes = new SearchNodes();
                for (final SearchNode node : mNodes.nodes)
                    if (node.pos < mPosLexiconSearchResults.length)
                        for (LexiconSearchResult r : mPosLexiconSearchResults[node.pos]) {
                            final SearchNode nextNode = new SearchNode();
                            nextNode.unitScore = r.unitResult.compare(unit);
                            if (nextNode.unitScore > 0) {
                                nextNode.score = nextNode.unitScore * r.unitResult.getInnerScore();
                                if (node.unitResult != null)
                                    nextNode.score += node.score + node.unitScore * nextNode.unitScore;
                                nextNode.length = node.length + r.length;
                                nextNode.pos = node.pos + r.length;
                                nextNode.unitResult = r.unitResult;
                                nextNode.prev = node;
                                nextNodes.nodes.add(nextNode);
                            }
                        }
                if (nextNodes.nodes.isEmpty())
                    return false;
                nextNodes.prev = mNodes;
                mNodes = nextNodes;
                ++mDepth;
                return true;
            }

            @Override
            public void popUnit() {
                --mDepth;
                mNodes = mNodes.prev;
            }

            @Override
            public void pushParams(final Collection<ItemParam> params) {
                mParamss.add(params);
            }

            @Override
            public void popParams(final Collection<ItemParam> params) {
                mParamss.remove(params);
            }

            @Override
            public Object beginSlot() {
                return (Integer) mDepth;
            }

            @Override
            public void pushSlot(final String name, final Object beginPos) {
                final ItemSlot slot = new ItemSlot();
                slot.name = name;
                slot.pos = (Integer) beginPos;
                slot.length = mDepth - slot.pos;
                mSlots.add(slot);
            }

            @Override
            public void popSlot() {
                mSlots.remove(mSlots.size() - 1);
            }

            @Override
            public void endOnePath() {
                for (SearchNode node : mNodes.nodes) {
                    double score = node.score / (Math.max(mText.length(), node.length) - 1);
                    if (score > mBestPathScore) {
                        mBestPathScore = score;
                        mBestPathDepth = mDepth;
                        mBestPath = node;
                        mBestPathParams.clear();
                        for (final Collection<ItemParam> params : mParamss)
                            mBestPathParams.addAll(params);
                        mBestPathSlots.clear();
                        mBestPathSlots.addAll(mSlots);
                    }
                }
            }
        }

        private void _logBestPath(SearchNode[] nodes) {
            log("Best path, score=" + mBestPathScore);
            for (int i = 0, n = nodes.length; i < n; ++i) {
                final SearchNode node = nodes[i];
                log("  unit[%d] %s - %s", i, node.unitResult.getId(), node.unitResult.getText());
            }
        }

        final SearchResult searchGrammar() {
            final long t0 = System.currentTimeMillis();
            _initSourceMatrix();
            mGrammarTree.newCursor(null).navigate(new SearchNavigator());
            final SearchNode[] nodes = new SearchNode[mBestPathDepth];
            for (SearchNode n = mBestPath; n != null; n = n.prev)
                if (n.unitResult != null)
                    nodes[--mBestPathDepth] = n;
            _logBestPath(nodes);

            final Map<String, String> slots = new HashMap<>();

            for (final ItemSlot slotInfo : mBestPathSlots) {
                final StringBuilder sb = new StringBuilder();
                for (int i = 0; i < slotInfo.length; ++i) {
                    final Unit.Result unitResult = nodes[slotInfo.pos + i].unitResult;
                    if (unitResult != null) {
                        final String unitText = unitResult.getText();
                        if (unitText != null)
                            sb.append(unitText);
                    }
                }
                if (sb.length() > 0) {
                    final String v = sb.toString();
                    slots.put(slotInfo.name, v);
                    log(" grammar slot [%d,%d] %s = %s", slotInfo.pos, slotInfo.length, slotInfo.name, v);
                }
            }

            for (final ItemParam param : mBestPathParams) {
                slots.put(param.key, param.value);
                log(" grammar param %s = %s", param.key, param.value);
            }

            for (final SearchNode node : nodes) {
                final Unit.Result unitResult = node.unitResult;
                if (unitResult == null)
                    continue;
                if (!(unitResult instanceof UnitLexiconSlotResult))
                    continue;
                final String unitText = unitResult.getText();
                if (unitText == null)
                    continue;
                final Lexicon lexicon = ((UnitLexiconSlotResult) unitResult).mLexicon;
                int id = lexicon.findItem(unitText);
                if (id >= 0)
                    for (final ItemParam param : lexicon.getItemParams(id)) {
                        String v = param.value;
                        if (v.equals("<0>"))
                            v = unitText;
                        slots.put(param.key, v);
                        log(" lexicon item param %s = %s", param.key, v);
                    }
            }

            for (;;) {
                boolean pending = false;
                for (final Map.Entry<String, String> slot : slots.entrySet()) {
                    final String v = slot.getValue();
                    if (v.startsWith("<") && v.endsWith(">")) {
                        final String v1 = slots.get(v.substring(1, v.length() - 1));
                        if (v1 == null)
                            throw new RuntimeException("slot " + v + " not found");
                        if (v1.startsWith("<") && v1.endsWith(">"))
                            pending = true;
                        else
                            slot.setValue(v1);
                    }
                }
                if (!pending)
                    break;
            }

            final Set<String> toRemoveSlots = new HashSet<String>();
            for (final String slot : slots.keySet())
                if (slot.startsWith("$") || slot.startsWith("Digit:"))
                    toRemoveSlots.add(slot);
            for (final String slot : toRemoveSlots)
                slots.remove(slot);

            long t = System.currentTimeMillis();
            log_result(" tree search finish, %.3fs time used", (t - t0) / 1000.);
            return new SearchResult(slots, mBestPathScore, (t - mTime0) / 1000.);
        }
    }

    private static final class SearchNode {
        SearchNode prev = null;
        int pos = 0, length = 0;
        Unit.Result unitResult = null;
        double unitScore = 0, score = 0;
    }

    private static final class SearchNodes {
        SearchNodes prev = null;
        final List<SearchNode> nodes = new ArrayList<>();
    }

    private static final class LexiconSearchResult {
        Unit.Result unitResult;
        int length;
    }

    private static final class LexiconSearchResultList extends ArrayList<LexiconSearchResult> {
        private static final long serialVersionUID = 1L;
    }

    /**
     * 搜索语法
     * @param text 待搜索的输入文本
     * @return 搜索结果
     */
    public final SearchResult search(final String text) {
        log_result("*** Searching for %s:", text);
        final SearchContext searchContext = new SearchContext(text);
        searchContext.searchLexicons(); // 先搜索各个词典
        final SearchResult rr = searchContext.searchGrammar(); // 再搜索语法树
        if (rr != null) {
            log_result("RESULT: %f", rr.score);
            for (final Map.Entry<String, String> param : rr.params.entrySet())
                log_result("  <%s>=%s", param.getKey(), param.getValue());
        } else {
            log_result("NO RESULT!");
        }
        return rr;
    }
}
