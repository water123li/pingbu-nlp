package pingbu.nlp;

public class UnitLexiconSlotResult extends UnitLexiconSlot implements
        Unit.Result {

    private final String mSourceText;
    private final Lexicon.SearchResult mResult;

    public UnitLexiconSlotResult(final Lexicon lexicon,
            final String sourceText, final Lexicon.SearchResult result) {
        super(lexicon);
        mSourceText = sourceText;
        mResult = result;
    }

    @Override
    public String getText() {
        if (mLexicon.getType() == Lexicon.TYPE_FUZZY)
            return mSourceText.substring(mResult.pos, mResult.pos
                    + mResult.length);
        else
            return mLexicon.getItemText(mResult.item);
    }

    @Override
    public int getLength() {
        return mLexicon.getItemText(mResult.item).length();
    }

    @Override
    public double getInnerScore() {
        return mResult.innerScore;
    }

    @Override
    public double compare(Unit unit) {
        if (unit instanceof UnitLexiconSlot
                && ((UnitLexiconSlot) unit).mLexicon.equals(mLexicon))
            return mResult.score;
        else
            return 0;
    }
}
