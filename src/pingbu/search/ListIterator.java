package pingbu.search;

import java.util.List;

public class ListIterator implements Index.Iterator {
    private final List<Integer> mIds;
    private int mPos = 0;

    public ListIterator(final List<Integer> ids) {
        mIds = ids;
    }

    @Override
    public int getNextItem() {
        if (mIds != null && mPos < mIds.size())
            return mIds.get(mPos);
        return Integer.MAX_VALUE;
    }

    @Override
    public double sumupToItem(final int id) {
        if (getNextItem() == id) {
            ++mPos;
            return 1;
        }
        return 0;
    }

    @Override
    public void close() throws Exception {
    }
}
