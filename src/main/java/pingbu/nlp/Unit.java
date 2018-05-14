package pingbu.nlp;

interface Unit {
    String getId();

    interface Result extends Unit {
        String getText();

        int getLength();

        double getInnerScore();

        double compare(Unit unit);
    }
}
