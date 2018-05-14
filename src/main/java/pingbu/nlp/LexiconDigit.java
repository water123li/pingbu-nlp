package pingbu.nlp;

import java.util.Collection;

/**
 * 数字词典
 * 
 * @author pingbu
 */
class LexiconDigit extends Lexicon {
    public static final int FLAG_ALL = -1;
    public static final int FLAG_ASC = 1;
    public static final int FLAG_ZH_CODE = 2;
    public static final int FLAG_ZH_VALUE = 4;

    private static final char[] ZhCode = "零一二三四五六七八九".toCharArray();

    private final Lexicon mLexicon;

    private static String __formatZhCode(int digit) {
        final char[] cs = Integer.toString(digit).toCharArray();
        for (int i = 0; i < cs.length; ++i)
            cs[i] = ZhCode[cs[i] - '0'];
        return new String(cs);
    }

    private static String __formatZhValueSegment(int digit) {
        final StringBuilder s = new StringBuilder();
        if (digit >= 1000) {
            s.append(ZhCode[digit / 1000]);
            s.append("千");
            digit %= 1000;
            if (digit < 100)
                s.append("零");
        }
        if (digit >= 100) {
            s.append(ZhCode[digit / 100]);
            s.append("百");
            digit %= 100;
            if (digit < 10)
                s.append("零");
        }
        if (digit >= 10) {
            s.append(ZhCode[digit / 10]);
            s.append("十");
            digit %= 10;
        }
        s.append(ZhCode[digit]);
        return s.toString();
    }

    private static String __formatZhValue(int digit) {
        final StringBuilder s = new StringBuilder();
        if (digit >= 100000000) {
            s.append(__formatZhValue(digit / 100000000));
            s.append("亿");
            digit %= 100000000;
            if (digit < 10000000)
                s.append("零");
        }
        if (digit >= 10000) {
            s.append(__formatZhValueSegment(digit / 10000));
            s.append("万");
            digit %= 10000;
            if (digit < 1000)
                s.append("零");
        }
        s.append(__formatZhValueSegment(digit));
        String t = s.toString();
        if (t.startsWith("一十"))
            t = t.substring(1);
        return t;
    }

    private static LexiconSimple __newLexicon(String name, int min, int max,
            int flags) {
        final LexiconSimple lexicon = new LexiconSimple1(String.format(name,
                min, max), false);
        if ((flags & FLAG_ASC) != 0) {
            for (int i = min; i <= max; ++i)
                lexicon.addItem(Integer.toString(i), name + "=" + i);
        }
        if ((flags & FLAG_ZH_CODE) != 0) {
            for (int i = min; i <= max; ++i)
                lexicon.addItem(__formatZhCode(i), name + "=" + i);
        }
        if ((flags & FLAG_ZH_VALUE) != 0) {
            for (int i = min; i <= max; ++i)
                lexicon.addItem(__formatZhValue(i), name + "=" + i);
        }
        return lexicon;
    }

    public LexiconDigit(String name, int min, int max) {
        this(name, min, max, FLAG_ALL);
    }

    public LexiconDigit(String name, int min, int max, int flags) {
        super(name);
        mLexicon = __newLexicon(name, min, max, flags);
    }

    @Override
    public final int getType() {
        return Lexicon.TYPE_NORMAL;
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
