package pingbu.nlp;

import pingbu.pinyin.Pinyin;

class UnitChar implements Unit {

    final char mChar;

    UnitChar(char c) {
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
