package pingbu.nlp;

class UnitLexiconSlot implements Unit {

    protected final Lexicon mLexicon;
    private final String mId;

    public UnitLexiconSlot(Lexicon lexicon) {
        mLexicon = lexicon;
        mId = String.format("<%s>", lexicon.name);
    }

    @Override
    public String toString() {
        return mId;
    }

    @Override
    public String getId() {
        return mId;
    }
}
