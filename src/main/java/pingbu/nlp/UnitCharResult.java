package pingbu.nlp;

import pingbu.pinyin.Pinyin;

class UnitCharResult extends UnitChar implements Unit.Result {

    UnitCharResult(final char c) {
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
    public double compare(final Unit unit) {
        if (unit instanceof UnitChar)
            return Pinyin.compareChar(mChar, ((UnitChar) unit).mChar);
        else
            return 0;
    }
}
