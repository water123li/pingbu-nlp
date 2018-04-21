import java.io.IOException;
import java.util.Map;

import pingbu.common.Pinyin;
import pingbu.nlp.Grammar;
import pingbu.nlp.LexiconSimple1;
import pingbu.nlp.Parser;

public class Example {

    private Grammar mGrammar = null;

    private void __loadSlot(Parser parser, String name, boolean fuzzy,
            String path) {
        try {
            parser.addSlot(name, LexiconSimple1.load(name, fuzzy, path));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Example() {
        String dataDir = "data";

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

        Parser parser = new Parser();

        dataDir += "/nlp/";
        __loadSlot(parser, "TVChannelName", false, dataDir + "TVChannel.txt");
        __loadSlot(parser, "VideoName", false, dataDir + "Video.txt");
        __loadSlot(parser, "Artist", false, dataDir + "Artist.txt");
        __loadSlot(parser, "Type", false, dataDir + "Type.txt");
        __loadSlot(parser, "Area", false, dataDir + "Area.txt");
        __loadSlot(parser, "City", false, dataDir + "City.txt");
        __loadSlot(parser, "StockName", false, dataDir + "Stock.txt");
        __loadSlot(parser, "Lesson", true, dataDir + "Lesson.txt");

        parser.addCompiledSlot("$Prefix", "[麻烦[你]]([请](帮|给|为)我)|我(想|要|想要)");

        parser.addCompiledSlot("Year", "(<Digit:1800-2099>|前|去|旧|今|明|后)年[度]");
        parser.addCompiledSlot("$Month",
                "<Digit:1-12>月[份]|(上上|上|这|下|下下)[个]月|本月");
        parser.addSlot("Month", "[<Year>[的]]<$Month>");
        parser.addCompiledSlot("Week", "(上上|上|这|下|下下)([个]星期|周)");
        parser.addCompiledSlot(
                "$Date",
                "<Digit:1-31>[日|号]|(前|昨|今|明|后)[日|天|朝]|[(上上|上|这|下|下下)[个]](星期|周)(一|二|三|四|五|六|日|天)");
        parser.addSlot("Date", "[<Month>[的]]<$Date>");
        parser.addSlot("$DateRange", "<Year>|<Month>|<Week>|<Date>");
        parser.addCompiledSlot("$Time",
                "[<Digit:0-23>(点|时)[整|半|一刻|差一刻|<Digit:1-59>分|多][钟]]");
        parser.addSlot("Time",
                "[凌晨|清晨|晨|早|早上|上午|中午|午后|下午|黄昏|傍晚|晚|晚上|夜|夜里|深夜|午夜|半夜|下半夜|后半夜]<$Time>");
        parser.addSlot("$DateTime", "<Date>|[<Date>[的]]<Time>");

        parser.addCompiledSlot("Category", "影视[剧]|电影|[影]片|[电视]剧|剧集|[电视]节目|动漫");
        parser.addCommand("[[<$Prefix>]打开](搜视榜|点播[应用])", "action=video");
        parser.addCompiledSlot("$VideoAction",
                "(查|找|查找|搜[索]|打开|点播|看|播|放|播放)[[一]下]");
        parser.addCompiledSlot("Order", "热门|热播|好看|好评|最新|最近");
        parser.addCommand(
                "[[<$Prefix>]<$VideoAction>][[([<Artist>[<Role>]]&[<Order>]&[<Area>]&[<$DateRange>])[的|嘅]][<Type>]<Category>][<VideoName>[节目|<Category>][<Episode>]]",
                "action=video");

        parser.addSlot("TVChannel",
                "<TVChannelName>|<Digit:1-99>[频道|台]|频道<Digit:1-99>|(上|下|前|后)[一]个[频道|台])");
        parser.addCommand(
                "[[<$Prefix>]([收]看]|播放)(电视|[电视]直播)|[[<$Prefix>]打开]直播[应用]",
                "action=tvlive");
        parser.addCommand("[[<$Prefix>]([收]看|打开|播放]<TVChannelCategory>[频道]",
                "action=tvlive");
        parser.addCommand("[[<$Prefix>]([收]看|打开|播放)]<TVChannel>[[的|嘅]节目]",
                "action=tvlive");
        parser.addCommand("[[<$Prefix>](调|跳|转|换|切[换])到]<TVChannel>",
                "action=tvlive");
        parser.addCommand("[<$Prefix>][打开]回看[应用]", "action=tvback");
        parser.addCommand("[<$Prefix>]回看[电视|<TVChannel>]", "action=tvback");
        parser.addCommand(
                "[[<$Prefix>](回看|打开|(调|跳|转|换|切[换])到)][<TVChannel>]<$DateTime>[[的|嘅]节目]",
                "action=tvback");

        parser.addSlot("Music",
                "<MusicName>|<Artist>(的|嘅)(歌[曲]|音乐)[<MusicName>]|<MusicType>");
        parser.addCommand("[[<$Prefix>](听|播|放|播放|打开)](歌[曲]|音乐|<Music>)",
                "action=music");
        parser.addCommand("[<$Prefix>](唱K|([演]唱|K)(歌[曲]|<Music>))",
                "action=ktv");

        parser.addCompiledSlot("$QueryAction",
                "(查|查询|查看|搜[索]|看|睇|问[你])[[一]下]|查查看|看看|问问看|问问你");
        parser.addSlot("$CityDate", "<City>|<Date>|<City><Date>|<Date><City>");
        parser.addCommand(
                "[[<$Prefix>]<$QueryAction>][<$CityDate>[的|嘅]]天气[情况|信息|怎[么]样|[还]好吗|好不好|可好]",
                "action=weather");
        parser.addSlot("$WeatherRainVerb", "下|降|落");
        parser.addSlot("$WeatherRainVerbQuery", "下不下|降不降|落不落");
        parser.addSlot("$WeatherRainType", "毛毛[细]|小|大|中|暴|雷|阵|雷阵");
        parser.addCommand(
                "[[<$Prefix>]<$QueryAction>][<$CityDate>][[的|嘅]天[气]][[会]<$WeatherRainVerb>[<$WeatherRainType>]雨吗|会不会<$WeatherRainVerb>[<$WeatherRainType>]雨[呢]|<$WeatherRainVerbQuery>[<$WeatherRainType>]雨[呢]]",
                "action=weather.rain");
        parser.addSlot("$WeatherWindVerb", "起|刮|吹|来");
        parser.addSlot("$WeatherWindVerbQuery", "起不起|刮不刮|吹不吹|来不来");
        parser.addSlot("$WeatherWindType", "阵|微|大|中|暴|台|飓|龙卷");
        parser.addCommand(
                "[[<$Prefix>]<$QueryAction>][<$CityDate>][[的|嘅]天[气]][[会]<$WeatherWindVerb>风吗|会不会<$WeatherWindVerb>风[呢]|<$WeatherWindVerbQuery>风[呢]]",
                "action=weather.wind");

        parser.addSlot("Stock", "[股票|证券]<StockName>|<StockName>[[的|嘅](股票|证券)]");
        parser.addSlot("StockTimeline", "分时");
        parser.addSlot("StockKlinePeriod", "(1|5|15|30|60)分钟|日|周|月|年");
        parser.addSlot("StockKline", "[<StockKlinePeriod>]K线");
        parser.addSlot("StockChart", "(<StockTimeline>|<StockKline>)[图]");
        parser.addCommand("[[<$Prefix>](打开|查看)](股市|股票|沪市|上交所|深市|深交所|创业板)[大盘]",
                "action=stock");
        parser.addCommand(
                "[[<$Prefix>](打开|查看)]<Stock>[[的|嘅][股价|价格|行情|公司|<StockChart>][情况|信息]]",
                "action=stock");

        parser.addCommand("[<$Prefix>][打开]翻译", "action=translate");
        parser.addCommand("[<$Prefix>](网购|[上网](购物|买东西|买买买))",
                "action=merchandise");
        parser.addCommand(
                "[[<$Prefix>]交][手机|[电]话|水|电|[煤|液化|天然]气|[宽带]([上]网|网络)|[有线]电视|收视]费",
                "action=交费");
        parser.addCommand("[<$Prefix>](上网[冲浪]|(打开|浏览|看)网页)", "action=website");

        parser.addCommand("[[<$Prefix>](打开|[查]看)]播放记录", "action=播放记录");
        parser.addCommand(
                "[[<$Prefix>](打开|(跳|翻|切)到)](第<Digit:1-200>|((上|下|前|后|第|最(前|后|末))[一])页)",
                "action=page");

        parser.addCommand("[[<$Prefix>](学[习])]<Lesson>[课文]", "action=learn");

        mGrammar = parser.compileGrammar();
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
        Example example = new Example();
        example.parse("我要看去年美国的电视剧");
        example.parse("麻烦帮我播放逻辑思维节目");
        example.parse("回看中央2台昨晚8点多钟的节目");
        example.parse("帮我查一下后天北京的天气");
        example.parse("我想学格萨尔王的课文");
    }
}
