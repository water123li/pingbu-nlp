package pingbu.nlp;

class SubtreeParamName extends Subtree {

    private String mParamName;
    private Subtree mSubtree;

    public SubtreeParamName(String paramName, Subtree subtree) {
        mParamName = paramName;
        mSubtree = subtree;
    }

    private class EnterCursor extends Subtree.Cursor {

        public EnterCursor(Subtree.Cursor returnCursor) {
            super(returnCursor);
        }

        @Override
        public void travel(TravelInfo info) {
            mSubtree.newCursor(new ExitCursor(returnCursor, info.path.size())).travel(info);
        }

        private class ExitCursor extends Subtree.Cursor {

            private int mParamPos;

            public ExitCursor(Subtree.Cursor returnCursor, int paramPos) {
                super(returnCursor);
                mParamPos = paramPos;
            }

            @Override
            public void travel(TravelInfo info) {
                Grammar.ItemParam param = new Grammar.ItemParam();
                param.name = mParamName;
                param.pos = mParamPos;
                param.length = info.path.size() - mParamPos;
                info.params.add(param);
                returnCursor.travel(info);
                info.params.remove(param);
            }
        }
    }

    @Override
    public Subtree.Cursor newCursor(Subtree.Cursor returnCursor) {
        return new EnterCursor(returnCursor);
    }
}
