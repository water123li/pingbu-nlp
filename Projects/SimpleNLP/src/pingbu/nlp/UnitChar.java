package pingbu.nlp;

import pingbu.common.Pinyin;

class UnitChar implements Unit {

    protected char mChar;

    public UnitChar(char c) {
        mChar = c;
    }

    @Override
    public String toString() {
        return Character.toString(mChar);
    }

    @Override
    public String getId() {
        return String.format("%d", Pinyin.normailizeChar(mChar));
    }
}
