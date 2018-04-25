package pingbu.nlp;

import java.util.Collection;

/**
 * 词典槽子树，引用词典并应用默认槽和参数信息
 * 
 * @author pingbu
 */
class SubtreeLexiconSlot extends Subtree {

    private final Lexicon mLexicon;
    private final UnitLexiconSlot mUnit;
    private final Collection<Grammar.ItemParam> mParams;

    public SubtreeLexiconSlot(Lexicon lexicon,
            Collection<Grammar.ItemParam> params) {
        mLexicon = lexicon;
        mUnit = new UnitLexiconSlot(lexicon);
        mParams = params;
    }

    private class Cursor extends Subtree.Cursor {
        public Cursor(Subtree.Cursor returnCursor) {
            super(returnCursor);
        }

        @Override
        public void navigate(Navigator navigator) {
            if (navigator.extendLexicon()) {
                for (int i = 0; i < mLexicon.getItemCount(); ++i) {
                    String s = mLexicon.getItemText(i);
                    for (int j = 0; j < s.length(); ++j)
                        if (!navigator.pushUnit(new UnitChar(s.charAt(j))))
                            throw new RuntimeException();
                    navigator.pushParams(mParams);
                    navigateReturn(navigator);
                    navigator.popParams(mParams);
                    for (int j = 0; j < s.length(); ++j)
                        navigator.popUnit();
                }
            } else if (navigator.pushUnit(mUnit)) {
                navigator.pushParams(mParams);
                navigateReturn(navigator);
                navigator.popParams(mParams);
                navigator.popUnit();
            }
        }
    }

    @Override
    public Subtree.Cursor newCursor(Subtree.Cursor returnCursor) {
        return new Cursor(returnCursor);
    }
}
