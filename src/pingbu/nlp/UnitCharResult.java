package pingbu.nlp;


class UnitCharResult extends UnitChar implements Unit.Result {

    public UnitCharResult(char c) {
        super(c);
    }

    @Override
    public String getText() {
        return Character.toString(mChar);
    }

    @Override
    public int getLength() {
        return 1;
    }

    @Override
    public double getInnerScore() {
        return 0;
    }

    @Override
    public double compare(Unit unit) {
        if (unit instanceof UnitChar)
            return Pinyin.compareChar(mChar, ((UnitChar) unit).mChar);
        else
            return 0;
    }
}
