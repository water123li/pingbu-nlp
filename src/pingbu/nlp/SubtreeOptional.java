package pingbu.nlp;

/**
 * 可选语法子树
 * 
 * @author pingbu
 */
class SubtreeOptional extends Subtree {

    private Subtree mSubtree;

    public SubtreeOptional(Subtree subtree) {
        mSubtree = subtree;
    }

    private class Cursor extends Subtree.Cursor {

        public Cursor(Subtree.Cursor returnCursor) {
            super(returnCursor);
        }

        @Override
        public void navigate(Navigator navigator) {
            mSubtree.newCursor(mReturnCursor).navigate(navigator);
            navigateReturn(navigator);
        }
    }

    @Override
    public Subtree.Cursor newCursor(Subtree.Cursor returnCursor) {
        return new Cursor(returnCursor);
    }
}
