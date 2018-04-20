package pingbu.nlp;

import java.util.Collection;

public class LexiconWrapper extends Lexicon {
    private Lexicon mLexicon = null;

    public LexiconWrapper(String debugName) {
        super(debugName);
    }

    public void setLexicon(Lexicon lexicon) {
        mLexicon = lexicon;
    }

    @Override
    public int getItemCount() {
        return mLexicon.getItemCount();
    }

    @Override
    public String getItemText(int id) {
        return mLexicon.getItemText(id);
    }

    @Override
    public Collection<Grammar.ItemParam> getItemParams(int id) {
        return mLexicon.getItemParams(id);
    }

    @Override
    public int findItem(String text) {
        return mLexicon.findItem(text);
    }

    @Override
    public Collection<SearchResult> search(String text) {
        return mLexicon.search(text);
    }
}
