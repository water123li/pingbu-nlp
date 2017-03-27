package pingbu.nlp;

class SubtreeLexiconSlot extends Subtree {

    private Lexicon mLexicon;

    public SubtreeLexiconSlot(Lexicon lexicon) {
        mLexicon = lexicon;
    }

    private class Cursor extends Subtree.Cursor {
        public Cursor(Subtree.Cursor returnCursor) {
            super(returnCursor);
        }

        @Override
        public void travel(TravelInfo info) {
            if (info.commandId != null) {
                info.path.add(new UnitLexiconSlot(mLexicon));
                returnCursor.travel(info);
                info.path.remove(info.path.size() - 1);
            } else
                for (int i = 0; i < mLexicon.getItemCount(); ++i) {
                    String s = mLexicon.getItem(i);
                    for (int j = 0; j < s.length(); ++j)
                        info.path.add(new UnitChar(s.charAt(j)));
                    returnCursor.travel(info);
                    for (int j = 0; j < s.length(); ++j)
                        info.path.remove(info.path.size() - 1);
                }
        }
    }

    @Override
    public Subtree.Cursor newCursor(Subtree.Cursor returnCursor) {
        return new Cursor(returnCursor);
    }
}
