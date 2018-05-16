package pingbu.nlp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import pingbu.logger.Logger;

/**
 * 语法解析模块，用于解析语法描述，生成语法树或词典
 * 
 * @author pingbu
 */
public final class Parser {
    private static final String TAG = Parser.class.getSimpleName();
    private static final boolean LOG = false;
    private static final boolean LOG_ITEM = false;

    private static void log(String fmt, Object... args) {
        if (LOG)
            Logger.d(TAG, fmt, args);
    }

    private static void log_item(String fmt, Object... args) {
        if (LOG_ITEM)
            Logger.d(TAG, fmt, args);
    }

    private final Map<String, Subtree> mSlots = new HashMap<>();
    private final Map<String, Lexicon> mLexicons = new HashMap<>();

    private final SubtreeOr mCommands = new SubtreeOr();

    private static ArrayList<Grammar.ItemParam> _parseParams(String desc) {
        final ArrayList<Grammar.ItemParam> params = new ArrayList<>();
        if (desc != null)
            for (String item : desc.split(",")) {
                final Grammar.ItemParam param = new Grammar.ItemParam();
                final int p = item.indexOf('=');
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

    private final class _Parser {
        private final String mDesc;
        private final Collection<Grammar.ItemParam> mParams;
        private int mPosition;

        _Parser(String desc, String params) {
            mDesc = desc;
            mParams = _parseParams(params);
            mPosition = 0;
        }

        final Subtree parse() {
            SubtreeOr orSubtree = null;
            SubtreeAnd andSubtree = null;
            SubtreeLinear linearSubtree = new SubtreeLinear();
            while (mPosition < mDesc.length()) {
                char c = mDesc.charAt(mPosition++);
                if (c == '(' || c == '[') {
                    linearSubtree.addSubtree(c == '[' ? new SubtreeOptional(parse()) : parse());
                } else if (c == ')' || c == ']') {
                    break;
                } else if (c == '<') {
                    linearSubtree.addSubtree(parseSlot());
                } else if (c == '|') {
                    if (orSubtree == null)
                        orSubtree = new SubtreeOr();
                    if (andSubtree != null) {
                        andSubtree.addSubtree(linearSubtree);
                        orSubtree.addSubtree(andSubtree);
                        andSubtree = null;
                    } else {
                        orSubtree.addSubtree(linearSubtree);
                    }
                    linearSubtree = new SubtreeLinear();
                } else if (c == '&') {
                    if (andSubtree == null)
                        andSubtree = new SubtreeAnd();
                    andSubtree.addSubtree(linearSubtree);
                    linearSubtree = new SubtreeLinear();
                } else
                    linearSubtree.addSubtree(new SubtreeChar(c));
            }
            Subtree subtree = linearSubtree;
            if (andSubtree != null) {
                andSubtree.addSubtree(subtree);
                subtree = andSubtree;
            }
            if (orSubtree != null) {
                orSubtree.addSubtree(subtree);
                subtree = orSubtree;
            }
            if (mParams != null && !mParams.isEmpty())
                subtree = new SubtreeParams(mParams, subtree);
            return subtree;
        }

        private Subtree parseSlot() {
            int a, b;
            a = b = mPosition;
            while (mDesc.charAt(b) != '>')
                ++b;
            mPosition = b + 1;
            final String slot = mDesc.substring(a, b);
            if (!mSlots.containsKey(slot)) {
                if (slot.startsWith("Digit:")) {
                    try {
                        final int p = slot.indexOf('-', 6);
                        final Lexicon lexicon = new LexiconDigit(slot, Integer.parseInt(slot.substring(6, p)), Integer.parseInt(slot.substring(p + 1)));
                        addSlot(slot, lexicon);
                    } catch (Exception e) {
                        Logger.e(TAG, "slot <" + slot + "> invalid");
                    }
                }
            }
            if (mSlots.containsKey(slot)) {
                return new SubtreeSlot(slot, mSlots.get(slot));
            } else {
                Logger.e(TAG, "slot <" + slot + "> not found");
                return new SubtreeEmpty();
            }
        }
    }

    public final void addCompiledSlot(String name, String desc) {
        log("==> addCompiledSlot %s <-- %s", name, desc);
        addSlot(name, __compileLexicon(name, 0, new _Parser(desc, null).parse()), null);
        log("<== addCompiledSlot %s", name);
    }

    public final void addSlot(String name, Lexicon lexicon) {
        addSlot(name, lexicon, null);
    }

    public final void addSlot(String name, Lexicon lexicon, String params) {
        log("==> addSlot %s <-- lexicon", name);
        mLexicons.put(name, lexicon);
        if (params != null)
            params = params.replace("<0>", '<' + name + '>');
        mSlots.put(name, new SubtreeLexiconSlot(lexicon, _parseParams(params)));
        log("<== addSlot %s", name);
    }

    public final void addSlot(String name, String desc) {
        addSlot(name, desc, null);
    }

    public final void addSlot(String name, String desc, String params) {
        log("==> addSlot %s <-- %s", name, desc);
        if (params != null)
            params = params.replace("<0>", '<' + name + '>');
        mSlots.put(name, new _Parser(desc, params).parse());
        log("<== addSlot %s", name);
    }

    public final void addCommand(String desc) {
        addCommand(desc, null);
    }

    public final void addCommand(String desc, String params) {
        log("==> addCommand <-- %s {%s}", desc, params);
        mCommands.addSubtree(new _Parser(desc, params).parse());
        log("<== addCommand");
    }

    //////////////////////////////////
    /// COMPILE LEXICON

    private static String pathToString(Iterable<?> path) {
        StringBuilder sb = new StringBuilder();
        for (Object unit : path)
            sb.append(unit.toString());
        return sb.toString();
    }

    private final class LexiconNavigator implements Subtree.Cursor.Navigator {
        private final LexiconSimple mLexicon;
        private final ArrayList<Unit> mPath = new ArrayList<>();
        private final ArrayList<Grammar.ItemSlot> mSlots = new ArrayList<>();
        private final ArrayList<Grammar.ItemParam> mParams = new ArrayList<>();

        LexiconNavigator(LexiconSimple lexicon) {
            mLexicon = lexicon;
        }

        @Override
        public final boolean extendLexicon() {
            return true;
        }

        @Override
        public final boolean pushUnit(Unit unit) {
            mPath.add(unit);
            return true;
        }

        @Override
        public final void popUnit() {
            mPath.remove(mPath.size() - 1);
        }

        @Override
        public final void pushParams(Collection<Grammar.ItemParam> params) {
            mParams.addAll(params);
        }

        @Override
        public final void popParams(Collection<Grammar.ItemParam> params) {
            mParams.removeAll(params);
        }

        @Override
        public final Object beginSlot() {
            return (Integer) mPath.size();
        }

        @Override
        public final void pushSlot(String name, Object beginPos) {
            Grammar.ItemSlot slot = new Grammar.ItemSlot();
            slot.name = name;
            slot.pos = (Integer) beginPos;
            slot.length = mPath.size() - slot.pos;
            mSlots.add(slot);
        }

        @Override
        public final void popSlot() {
            mSlots.remove(mSlots.size() - 1);
        }

        @Override
        public final void endOnePath() {
            if (!mPath.isEmpty()) {
                String s = pathToString(mPath);
                log_item("add item %s", s);
                mLexicon.addItem(s);
            }
        }
    }

    public final LexiconSimple compileLexicon(String name, int type) {
        return __compileLexicon(name, type, mCommands);
    }

    private LexiconSimple __compileLexicon(String name, int type, Subtree grammar) {
        log("==> compileLexicon %s", name);
        long t = System.currentTimeMillis();
        LexiconSimple lexicon = type == 2 ? new LexiconSimple2(name) : new LexiconSimple1(name, type == Lexicon.TYPE_FUZZY);
        grammar.newCursor(null).navigate(new LexiconNavigator(lexicon));
        t = System.currentTimeMillis() - t;
        log("<== compileLexicon %.3fs", t / 1000.);
        return lexicon;
    }

    public static LexiconSimple compileLexicon(String name, String desc) {
        return compileLexicon(name, desc, 0);
    }

    public static LexiconSimple compileLexicon(String name, String desc, int type) {
        Parser parser = new Parser();
        parser.addCommand(desc, null);
        return parser.compileLexicon(name, type);
    }

    /**
     * 编译语法
     *
     * @return 语法对象
     */
    public final Grammar compileGrammar() {
        return new Grammar(mCommands, mLexicons.values());
    }
}
