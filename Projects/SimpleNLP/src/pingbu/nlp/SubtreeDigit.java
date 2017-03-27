package pingbu.nlp;

class SubtreeDigit extends Subtree {

    private int mMin, mMax;

    public SubtreeDigit(int min, int max) {
        mMin = min;
        mMax = max;
    }

    private class Cursor extends Subtree.Cursor {
        public Cursor(Subtree.Cursor returnCursor) {
            super(returnCursor);
        }

        @Override
        public void travel(TravelInfo info) {
            for (int value = mMin; value <= mMax; ++value) {
                String s = Integer.toString(value);
                for (int i = 0; i < s.length(); ++i)
                    info.path.add(new UnitChar(s.charAt(i)));
                returnCursor.travel(info);
                for (int i = 0; i < s.length(); ++i)
                    info.path.remove(info.path.size() - 1);
            }
        }
    }

    @Override
    public Subtree.Cursor newCursor(Subtree.Cursor returnCursor) {
        return new Cursor(returnCursor);
    }
}
