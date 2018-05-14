package pingbu.nlp;

import java.util.LinkedList;
import java.util.List;

/**
 * 或语法子树
 * 
 * @author pingbu
 */
class SubtreeOr extends Subtree {
    private List<Subtree> mSubtrees = new LinkedList<Subtree>();

    public SubtreeOr(Subtree... subtrees) {
        addSubtree(subtrees);
    }

    public void addSubtree(Subtree... subtrees) {
        for (Subtree subtree : subtrees)
            mSubtrees.add(subtree);
    }

    private class Cursor extends Subtree.Cursor {
        public Cursor(Subtree.Cursor returnCursor) {
            super(returnCursor);
        }

        @Override
        public void navigate(Navigator navigator) {
            if (mSubtrees.size() == 0)
                navigateReturn(navigator);
            else
                for (Subtree grammar : mSubtrees)
                    grammar.newCursor(mReturnCursor).navigate(navigator);
        }
    }

    @Override
    public Subtree.Cursor newCursor(Subtree.Cursor returnCursor) {
        return new Cursor(returnCursor);
    }
}
