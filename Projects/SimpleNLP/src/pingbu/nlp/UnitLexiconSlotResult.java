package pingbu.nlp;

public class UnitLexiconSlotResult extends UnitLexiconSlot implements Unit.Result {

    private Lexicon.SearchResult mResult;

    public UnitLexiconSlotResult(Lexicon lexicon, Lexicon.SearchResult result) {
        super(lexicon);
        mResult = result;
    }

    @Override
    public int getSourcePos() {
        return mResult.pos;
    }

    @Override
    public int getSourceLength() {
        return mResult.length;
    }

    @Override
    public String getText() {
        return mLexicon.getItem(mResult.item);
    }

    @Override
    public int getLength() {
        return mLexicon.getItem(mResult.item).length();
    }

    @Override
    public double getInnerScore() {
        return mResult.innerScore;
    }

    @Override
    public double compare(Unit unit) {
        if (unit instanceof UnitLexiconSlot && ((UnitLexiconSlot) unit).mLexicon.equals(mLexicon))
            return mResult.priority;
        else
            return 0;
    }
}
