package pingbu.nlp;

/**
 * 简单词典基类，增加了添加词条的API
 * 
 * @author pingbu
 */
public abstract class LexiconSimple extends Lexicon {
    public LexiconSimple() {
        super();
    }

    public LexiconSimple(String debugName) {
        super(debugName);
    }

    public abstract void addItems(String[] items);

    public abstract void addItems(Iterable<String> items);

    public abstract void addItem(String text);

    public abstract void addItem(String text, String params);
}
