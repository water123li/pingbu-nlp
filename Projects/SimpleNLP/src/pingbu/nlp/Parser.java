package pingbu.nlp;

import java.util.HashMap;
import java.util.Map;

import pingbu.nlp.Subtree.Cursor.TravelInfo;


public class Parser {

    private static void log(String fmt, Object... args) {
        // System.out.printf("[%d] ", System.currentTimeMillis());
        // System.out.printf(fmt + "\n", args);
    }

    private Map<String, Subtree> mSlots = new HashMap<String, Subtree>();
    private Map<String, Lexicon> mLexicons = new HashMap<String, Lexicon>();

    private SubtreeOr mCommands = new SubtreeOr();

    private class _Parser {
        private String mDesc;
        private int mPosition;

        public _Parser(String desc) {
            mDesc = desc;
            mPosition = 0;
        }

        public Subtree parse() {
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
            return subtree;
        }

        private Subtree parseSlot() {
            int a, b;
            a = b = mPosition;
            while (mDesc.charAt(b) != '>')
                ++b;
            mPosition = b + 1;
            String slot = mDesc.substring(a, b);
            if (slot.startsWith("Digit:")) {
                a = 6;
                b = slot.indexOf("-", a);
                if (b < 0) { // throw new Exception("slot <" + slot +
                             // "> invalid");
                    log("slot <" + slot + "> invalid");
                    return new SubtreeEmpty();
                }
                return new SubtreeDigit(Integer.parseInt(slot.substring(a, b)), Integer.parseInt(slot
                        .substring(b + 1)));
            } else if (mSlots.containsKey(slot))
                return new SubtreeParamName(slot, mSlots.get(slot));
            else if (mLexicons.containsKey(slot))
                return new SubtreeParamName(slot, new SubtreeLexiconSlot(mLexicons.get(slot)));
            else {
                // throw new Exception("slot <" + slot + "> not found");
                log("slot <" + slot + "> not found");
                return new SubtreeEmpty();
            }
        }
    }

    public void addSlot(String name, Lexicon lexicon) {
        mLexicons.put(name, lexicon);
    }

    public void addSlot(String name, String desc) {
        mSlots.put(name, new _Parser(desc).parse());
    }

    public void addCommand(String id, String desc) {
        mCommands.addSubtree(new SubtreeCommandId(id, new _Parser(desc).parse()));
    }

    // ////////////////////////////////
    // / COMPILE LEXICON

    private class EndLexiconCursor extends Subtree.Cursor {

        private Lexicon mLexicon;

        public EndLexiconCursor(Lexicon lexicon) {
            super(null);
            mLexicon = lexicon;
        }

        @Override
        public void travel(TravelInfo info) {
            if (!info.path.isEmpty()) {
                String s = Grammar.pathToString(info.path);
                log("add item %s", s);
                mLexicon.addItem(s);
            }
        }
    }

    public Lexicon compileLexicon() {
        Lexicon lexicon = new Lexicon();
        mCommands.newCursor(new EndLexiconCursor(lexicon)).travel(new TravelInfo());
        return lexicon;
    }

    public static Lexicon compileLexicon(String desc) {
        Parser grammar = new Parser();
        grammar.addCommand(null, desc);
        return grammar.compileLexicon();
    }

    // ////////////////////////////////
    // / COMPILE GRAMMAR

    private class EndGrammarCursor extends Subtree.Cursor {

        private Grammar mGrammar;

        public EndGrammarCursor(Grammar grammar) {
            super(null);
            mGrammar = grammar;
        }

        @Override
        public void travel(TravelInfo info) {
            log("%s", Grammar.pathToString(info.path));
            for (Grammar.ItemParam param : info.params)
                log("  <%s> %d,%d", param.name, param.pos, param.length);
            mGrammar.addPath(info.commandId, info.path.toArray(new Unit[info.path.size()]),
                    info.params.toArray(new Grammar.ItemParam[info.params.size()]));
        }
    }

    public Grammar compileGrammar() {
        Grammar grammar = new Grammar();
        for (String name : mLexicons.keySet())
            grammar.addLexicon(name, mLexicons.get(name));
        mCommands.newCursor(new EndGrammarCursor(grammar)).travel(new TravelInfo());
        return grammar;
    }
}
