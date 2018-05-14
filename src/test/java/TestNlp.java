import java.util.Map;

import pingbu.common.JarStorage;
import pingbu.nlp.Grammar;
import pingbu.nlp.NlpLoader;

public class TestNlp {

    private final Grammar mGrammar;

    private TestNlp() {
        System.out.print("Loading grammar...\n");
        long t = System.currentTimeMillis();
        mGrammar = NlpLoader.loadGrammar(new JarStorage(Test.class.getClassLoader()), "Grammar.txt");
        t = System.currentTimeMillis() - t;
        System.out.printf(" duration: %.3fs\n", t / 1000.);
    }

    private void parse(String text) {
        long t = System.currentTimeMillis();
        final Grammar.SearchResult r = mGrammar.search(text);
        t = System.currentTimeMillis() - t;
        System.out.print("==================================================\n");
        System.out.printf("Text: %s\n", text);
        if (r != null) {
            System.out.printf("Result: %.3f\n", r.score);
            for (final Map.Entry<String, String> param : r.params.entrySet())
                System.out.printf("  %s=%s\n", param.getKey(), param.getValue());
        } else {
            System.out.print("Result: null\n");
        }
        System.out.printf(" duration: %.3fs\n", t / 1000.);
    }

    public static void test() {
        final TestNlp obj = new TestNlp();
        obj.parse("帮我查一下后天北京的天气");
        obj.parse("回看中央2台昨晚8点多钟的节目");
        obj.parse("麻烦帮我播放逻辑思维节目");
        obj.parse("我要点播闯关节目");
        obj.parse("我享看2010年代的美国科幻片移动迷宫");
    }
}
