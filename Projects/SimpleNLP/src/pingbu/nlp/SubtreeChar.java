package pingbu.nlp;

/**
 * 字符槽子树
 * 
 * @author pingbu
 */
class SubtreeChar extends Subtree {

    private UnitChar mUnit;

    public SubtreeChar(char c) {
        mUnit = new UnitChar(c);
    }

    private class Cursor extends Subtree.Cursor {
        public Cursor(Subtree.Cursor returnCursor) {
            super(returnCursor);
        }

        @Override
        public void navigate(Navigator navigator) {
            if (navigator.pushUnit(mUnit)) {
                navigateReturn(navigator);
                navigator.popUnit();
            }
        }
    }

    @Override
    public Subtree.Cursor newCursor(Subtree.Cursor returnCursor) {
        return new Cursor(returnCursor);
    }
}
