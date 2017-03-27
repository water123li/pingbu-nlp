package pingbu.nlp;

class SubtreeCommandId extends Subtree {

    private String mCommandId;
    private Subtree mSubtree;

    public SubtreeCommandId(String commandId, Subtree subtree) {
        mCommandId = commandId;
        mSubtree = subtree;
    }

    private class Cursor extends Subtree.Cursor {

        public Cursor(Subtree.Cursor returnCursor) {
            super(returnCursor);
        }

        @Override
        public void travel(TravelInfo info) {
            info.commandId = mCommandId;
            mSubtree.newCursor(returnCursor).travel(info);
            info.commandId = null;
        }
    }

    @Override
    public Subtree.Cursor newCursor(Subtree.Cursor returnCursor) {
        return new Cursor(returnCursor);
    }
}
