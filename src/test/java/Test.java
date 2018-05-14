import pingbu.common.Pinyin;

public class Test {

    public static void main(String[] args) {
        // Logger.setLogger(new ConsoleLogger(true));
        Pinyin.init();
        TestNlp.test();
        TestSearch.test();
    }
}
