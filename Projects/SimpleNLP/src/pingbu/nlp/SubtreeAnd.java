package pingbu.nlp;

import java.util.LinkedList;
import java.util.List;

class SubtreeAnd extends Subtree {
    private List<Subtree> mSubtrees = new LinkedList<Subtree>();

    public SubtreeAnd(Subtree... subtrees) {
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
            else if (mSubtrees.size() == 1)
                mSubtrees.get(0).newCursor(returnCursor).travel(info);
            else
                for (int i = 0; i < mSubtrees.size(); ++i) {
                    SubtreeAnd g = new SubtreeAnd();
                    for (int j = 0; j < mSubtrees.size(); ++j)
                        if (j != i)
                            g.addSubtree(mSubtrees.get(j));
                    mSubtrees.get(i).newCursor(g.newCursor(returnCursor)).travel(info);
                }
        }
    }

    @Override
    public Subtree.Cursor newCursor(Subtree.Cursor returnCursor) {
        return new Cursor(returnCursor);
    }
}
