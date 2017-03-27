package pingbu.nlp;

import java.util.LinkedList;
import java.util.List;

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
        public void travel(TravelInfo info) {
            if (mSubtrees.size() == 0)
                returnCursor.travel(info);
            else
                for (Subtree grammar : mSubtrees)
                    grammar.newCursor(returnCursor).travel(info);
        }
    }

    @Override
    public Subtree.Cursor newCursor(Subtree.Cursor returnCursor) {
        return new Cursor(returnCursor);
    }
}
