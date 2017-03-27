package pingbu.nlp;

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
        public void travel(TravelInfo info) {
            mSubtree.newCursor(returnCursor).travel(info);
            returnCursor.travel(info);
        }
    }

    @Override
    public Subtree.Cursor newCursor(Subtree.Cursor returnCursor) {
        return new Cursor(returnCursor);
    }
}
