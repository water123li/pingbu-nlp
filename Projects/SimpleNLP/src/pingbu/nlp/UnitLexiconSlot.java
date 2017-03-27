package pingbu.nlp;

public class UnitLexiconSlot implements Unit {

    protected Lexicon mLexicon;
    private String mId;

    public UnitLexiconSlot(Lexicon lexicon) {
        mLexicon = lexicon;
        mId = String.format("<%d>", lexicon.getId());
    }

    public short getLexiconId() {
        return mLexicon.getId();
    }

    @Override
    public String toString() {
        return String.format("<%s>", mLexicon.getDebugName());
    }

    @Override
    public String getId() {
        return mId;
    }
}
