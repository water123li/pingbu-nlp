package pingbu.search;

import java.util.List;

class MultiIterator implements SearchIndex.Iterator {
    private final List<SearchIndex.Iterator> mIterators;

    public MultiIterator(final List<SearchIndex.Iterator> iterators) {
        mIterators = iterators;
    }

    @Override
    public int getNextItem() {
        int id = Integer.MAX_VALUE;
        for (final SearchIndex.Iterator iterator : mIterators)
            id = Math.min(id, iterator.getNextItem());
        return id;
    }

    @Override
    public double sumUpToItem(final int id) {
        double score = 0;
        for (final SearchIndex.Iterator iterator : mIterators)
            score = Math.max(score, iterator.sumUpToItem(id));
        return score;
    }

    @Override
    public void close() {
    }
}
