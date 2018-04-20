package pingbu.nlp;

/**
 * 空子树，用于解析到语法描述错误时为语法树提供容错
 * 
 * @author pingbu
 */
class SubtreeEmpty extends Subtree {

    private static class Cursor extends Subtree.Cursor {
        public Cursor(Subtree.Cursor returnCursor) {
            super(returnCursor);
        }

        @Override
        public void navigate(Navigator navigator) {
            navigateReturn(navigator);
        }
    }

    @Override
    public Subtree.Cursor newCursor(Subtree.Cursor returnCursor) {
        return new Cursor(returnCursor);
    }
}
