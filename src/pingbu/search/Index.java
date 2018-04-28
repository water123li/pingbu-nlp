package pingbu.search;

public interface Index {

    public interface Iterator {
        int getNextItem();

        double sumupToItem(int id);
    }

    void addItem(int id, String value);

    Iterator iterate(String value);
}
