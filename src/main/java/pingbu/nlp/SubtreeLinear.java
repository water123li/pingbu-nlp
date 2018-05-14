package pingbu.nlp;

import java.util.ArrayList;
import java.util.List;

/**
 * 线性语法子树
 * 
 * @author pingbu
 */
class SubtreeLinear extends Subtree {

    private List<Subtree> mSubtrees = new ArrayList<Subtree>();

    public void addSubtree(Subtree subtree) {
        mSubtrees.add(subtree);
    }

    private class Cursor extends Subtree.Cursor {
        private int mPosition = 0;

        public Cursor(Subtree.Cursor returnCursor) {
            super(returnCursor);
        }

        public Subtree.Cursor next() {
            Cursor cursor = new Cursor(mReturnCursor);
            cursor.mPosition = mPosition + 1;
            return cursor;
        }

        @Override
        public void navigate(Navigator navigator) {
            if (mPosition < mSubtrees.size())
                mSubtrees.get(mPosition).newCursor(next()).navigate(navigator);
            else
                navigateReturn(navigator);
        }
    }

    @Override
    public Subtree.Cursor newCursor(Subtree.Cursor returnCursor) {
        return new Cursor(returnCursor);
    }
}
