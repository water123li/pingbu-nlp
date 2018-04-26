import java.util.Map;

import pingbu.common.Pinyin;
import pingbu.nlp.Grammar;
import pingbu.nlp.NlpFile;

public class PingbuNlpExample {

    private Grammar mGrammar = null;

    public PingbuNlpExample() {
        final String dataDir = "data";
        Pinyin.init(dataDir + "/common");
        System.out.printf("Loading grammar...\n");
        long t = System.currentTimeMillis();
        mGrammar = NlpFile.loadGrammar(dataDir + "/nlp/Grammar.txt");
        t = System.currentTimeMillis() - t;
        System.out.printf(" duration: %.3fs\n", t / 1000.);
    }

    public void parse(String text) {
        long t = System.currentTimeMillis();
        final Grammar.SearchResult r = mGrammar.search(text);
        t = System.currentTimeMillis() - t;
        System.out
                .printf("==================================================\n");
        System.out.printf("Text: %s\n", text);
        if (r != null) {
            System.out.printf("Result: %.3f\n", r.score);
            for (final Map.Entry<String, String> param : r.params.entrySet()) {
                final String key = param.getKey();
                if (!key.startsWith("$") && !key.startsWith("Digit:"))
                    System.out.printf("  %s=%s\n", key, param.getValue());
            }
        } else {
            System.out.printf("Result: null\n");
        }
        System.out.printf(" duration: %.3fs\n", t / 1000.);
    }

    public static void main(String[] args) {
        final PingbuNlpExample example = new PingbuNlpExample();
        example.parse("我享看去年美国的电视剧");
        example.parse("麻烦帮我播放逻辑思维节目");
        example.parse("我要点播闯关节目");
        example.parse("回看中央2台昨晚8点多钟的节目");
        example.parse("帮我查一下后天北京的天气");
    }
}
