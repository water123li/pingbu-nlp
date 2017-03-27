package pingbu.nlp;

class SubtreeEmpty extends Subtree {

    private static class Cursor extends Subtree.Cursor {
        public Cursor(Subtree.Cursor returnCursor) {
            super(returnCursor);
        }

        @Override
        public void travel(TravelInfo info) {
            returnCursor.travel(info);
        }
    }

    @Override
    public Subtree.Cursor newCursor(Subtree.Cursor returnCursor) {
        return new Cursor(returnCursor);
    }
}
