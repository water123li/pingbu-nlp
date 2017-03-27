package pingbu.nlp;

import pingbu.common.Pinyin;

class UnitCharResult extends UnitChar implements Unit.Result {

    private int mPosition;

    public UnitCharResult(int pos, char c) {
        super(c);
        mPosition = pos;
    }

    @Override
    public int getSourcePos() {
        return mPosition;
    }

    @Override
    public int getSourceLength() {
        return 1;
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
