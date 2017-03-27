package pingbu.nlp;

interface Unit {
    public String getId();

    interface Result extends Unit {
        public String getText();

        public int getLength();

        public double getInnerScore();

        public double compare(Unit unit);

        public int getSourcePos();

        public int getSourceLength();
    }
}
