package pingbu.nlp;

/**
 * 语法槽子树，对下级子树产生槽信息
 * 
 * @author pingbu
 */
class SubtreeSlot extends Subtree {

    private final String mName;
    private final Subtree mSubtree;

    public SubtreeSlot(String name, Subtree subtree) {
        mName = name;
        mSubtree = subtree;
    }

    private class EnterCursor extends Subtree.Cursor {

        public EnterCursor(Subtree.Cursor returnCursor) {
            super(returnCursor);
        }

        @Override
        public void navigate(Navigator navigator) {
            mSubtree.newCursor(
                    new ExitCursor(mReturnCursor, navigator.beginSlot()))
                    .navigate(navigator);
        }

        private class ExitCursor extends Subtree.Cursor {

            private Object mBeginPos;

            public ExitCursor(Subtree.Cursor returnCursor, Object beginPos) {
                super(returnCursor);
                mBeginPos = beginPos;
            }

            @Override
            public void navigate(Navigator navigator) {
                navigator.pushSlot(mName, mBeginPos);
                navigateReturn(navigator);
                navigator.popSlot();
            }
        }
    }

    @Override
    public Subtree.Cursor newCursor(Subtree.Cursor returnCursor) {
        return new EnterCursor(returnCursor);
    }
}
