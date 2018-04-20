package pingbu.nlp;

import java.util.Collection;

/**
 * 参数语法子树，对下级子树产生参数表达式信息
 * 
 * @author pingbu
 */
class SubtreeParams extends Subtree {

    private final Collection<Grammar.ItemParam> mParams;
    private final Subtree mSubtree;

    public SubtreeParams(Collection<Grammar.ItemParam> params, Subtree subtree) {
        mParams = params;
        mSubtree = subtree;
    }

    private class Cursor extends Subtree.Cursor {

        public Cursor(Subtree.Cursor returnCursor) {
            super(returnCursor);
        }

        @Override
        public void navigate(Navigator navigator) {
            navigator.pushParams(mParams);
            mSubtree.newCursor(mReturnCursor).navigate(navigator);
            navigator.popParams(mParams);
        }
    }

    @Override
    public Subtree.Cursor newCursor(Subtree.Cursor returnCursor) {
        return new Cursor(returnCursor);
    }
}
