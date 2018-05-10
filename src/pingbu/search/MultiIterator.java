package pingbu.search;

import java.util.List;

public class MultiIterator implements Index.Iterator {
    private final List<Index.Iterator> mIterators;

    public MultiIterator(final List<Index.Iterator> iterators) {
        mIterators = iterators;
    }

    @Override
    public int getNextItem() {
        int id = Integer.MAX_VALUE;
        for (final Index.Iterator iterator : mIterators)
            id = Math.min(id, iterator.getNextItem());
        return id;
    }

    @Override
    public double sumupToItem(final int id) {
        double score = 0;
        for (final Index.Iterator iterator : mIterators)
            score = Math.max(score, iterator.sumupToItem(id));
        return score;
    }

    @Override
    public void close() throws Exception {
    }
}
