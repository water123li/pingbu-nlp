package pingbu.nlp;

public class UnitLexiconSlot implements Unit {

    protected final Lexicon mLexicon;
    private final String mId;

    public UnitLexiconSlot(Lexicon lexicon) {
        mLexicon = lexicon;
        mId = String.format("<%s>", lexicon.id);
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
