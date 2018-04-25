import java.io.IOException;
import java.util.Map;

import pingbu.common.Pinyin;
import pingbu.nlp.Grammar;
import pingbu.nlp.NlpFile;

public class PingbuNlpExample {

    private Grammar mGrammar = null;

    public PingbuNlpExample() {
        final String dataDir = "data";
        final String modalPath = dataDir + "/common/Pinyin.modal";
        try {
            Pinyin.loadModal(modalPath);
        } catch (IOException e1) {
            try {
                Pinyin.createModal(dataDir + "/common/Unihan_Readings.txt");
                try {
                    Pinyin.saveModal(modalPath);
                } catch (IOException e) {
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
        System.out.printf("Loading grammar...\n");
        long t = System.currentTimeMillis();
        mGrammar = NlpFile.loadGrammar(dataDir + "/nlp/Grammar.txt");
        t = System.currentTimeMillis() - t;
        System.out.printf(" duration: %.3fs\n", t / 1000.);
    }

    public void parse(String text) {
        long t = System.currentTimeMillis();
        Grammar.SearchResult r = mGrammar.search(text);
        t = System.currentTimeMillis() - t;
        System.out
                .printf("==================================================\n");
        System.out.printf("Text: %s\n", text);
        if (r != null) {
            System.out.printf("Result: %.3f\n", r.score);
            for (Map.Entry<String, String> param : r.params.entrySet())
                if (!param.getKey().startsWith("$"))
                    System.out.printf("  %s=%s\n", param.getKey(),
                            param.getValue());
        } else {
            System.out.printf("Result: null\n");
        }
        System.out.printf(" duration: %.3fs\n", t / 1000.);
    }

    public static void main(String[] args) {
        PingbuNlpExample example = new PingbuNlpExample();
        example.parse("我享看去年美国的电视剧");
        example.parse("麻烦帮我播放逻辑思维节目");
        example.parse("我要点播新闻节目");
        example.parse("回看中央2台昨晚8点多钟的节目");
        example.parse("帮我查一下后天北京的天气");
    }
}
