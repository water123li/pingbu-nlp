package pingbu.nlp;

class SubtreeChar extends Subtree {

    private char mChar;

    public SubtreeChar(char c) {
        mChar = c;
    }

    private class Cursor extends Subtree.Cursor {
        public Cursor(Subtree.Cursor returnCursor) {
            super(returnCursor);
        }

        @Override
        public void travel(TravelInfo info) {
            info.path.add(new UnitChar(mChar));
            returnCursor.travel(info);
            info.path.remove(info.path.size() - 1);
        }
    }

    @Override
    public Subtree.Cursor newCursor(Subtree.Cursor returnCursor) {
        return new Cursor(returnCursor);
    }
}
