package pingbu.nlp;


class UnitChar implements Unit {

    protected final char mChar;

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
