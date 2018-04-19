import java.io.IOException;
import java.util.Map;

import pingbu.common.Pinyin;
import pingbu.nlp.Grammar;
import pingbu.nlp.Lexicon;
import pingbu.nlp.Parser;

public class Example {

    private Grammar mGrammar = null;

    public Example() {
        String dataDir = "data";

        try {
            Pinyin.init(dataDir + "/common");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        Parser parser = new Parser();

        dataDir += "/nlp/";
        parser.addSlot("TVChannelName", Lexicon.load(dataDir + "TVChannel.txt"));
        parser.addSlot("VideoName", Lexicon.load(dataDir + "Video.txt"));
        parser.addSlot("Artist", Lexicon.load(dataDir + "Artist.txt"));
        parser.addSlot("Type", Lexicon.load(dataDir + "Type.txt"));
        parser.addSlot("Area", Lexicon.load(dataDir + "Area.txt"));
        parser.addSlot("City", Lexicon.load(dataDir + "City.txt"));
        parser.addSlot("StockName", Lexicon.load(dataDir + "Stock.txt"));

        parser.addSlot("Year", Parser.compileLexicon("(<Digit:1800-2100>|前|去|旧|今|明|后)年[度]"));
        parser.addSlot("Month", Parser.compileLexicon("[<Year>]<Digit:1-12>月[份]|(上上|上|这|下|下下)[个]月|本月"));
        parser.addSlot("Week", "(上上|上|这|下|下下)([个]星期|周)");
        parser.addSlot(
                "Date",
                Parser.compileLexicon("[<Month>]<Digit:1-31>[日|号]|(前|昨|今|明|后)(日|天|朝)|[(上上|上|这|下|下下)[个]](星期|周)(一|二|三|四|五|六|日|天)"));
        parser.addSlot("DateRange", "<Year>|<Month>|<Week>|<Date>");

        parser.addSlot("DateTime", "<Date>|<Time>|<Date><Time>");

        parser.addSlot("Prefix", Parser.compileLexicon("[麻烦[你]]([请](帮|给|为)我)|我(想|要|想要)"));

        parser.addSlot("Category", Parser.compileLexicon("影视[剧]|电影|[影]片|[电视]剧|剧集|[电视]节目|动漫"));
        parser.addCommand("video", "[[<Prefix>]打开](搜视榜|点播[应用])");
        parser.addSlot("VideoAction", Parser.compileLexicon("(查|找|查找|搜[索]|打开|点播|看|播|放|播放)[[一]下]"));
        parser.addSlot("Order", Parser.compileLexicon("热门|热播|好看|好评|最新|最近"));
        parser.addCommand(
                "video",
                "[[<Prefix>]<VideoAction>][[([<Artist>[<Role>]]&[<Order>]&[<Area>]&[<DateRange>])[的|嘅]][<Type>]<Category>][<VideoName>[节目|<Category>][<Episode>]]");

        parser.addSlot("TVChannel", "<TVChannelName>|<Digit:1-99>[频道|台]|频道<Digit:1-99>|(上|下|前|后)[一]个[频道|台])");
        parser.addCommand("tvlive", "[[<Prefix>]([收]看]|播放)(电视|[电视]直播)|[[<Prefix>]打开]直播[应用]");
        parser.addCommand("tvlive", "[[<Prefix>]([收]看|打开|播放]<TVChannelCategory>[频道]");
        parser.addCommand("tvlive", "[[<Prefix>]([收]看|打开|播放)]<TVChannel>[[的|嘅]节目]");
        parser.addCommand("tvlive", "[[<Prefix>](调|跳|转|换|切[换])到]<TVChannel>");
        parser.addCommand("tvback", "[<Prefix>][打开]回看[应用]");
        parser.addCommand("tvback", "[<Prefix>]回看[电视|<TVChannel>]");
        parser.addCommand("tvback", "[[<Prefix>](回看|打开|(调|跳|转|换|切[换])到)][<TVChannel>]<DateTime>[[的|嘅]节目]");

        parser.addSlot("Music", "<MusicName>|<Artist>(的|嘅)(歌[曲]|音乐)[<MusicName>]|<MusicType>");
        parser.addCommand("music", "[[<Prefix>](听|播|放|播放|打开)](歌[曲]|音乐|<Music>)");
        parser.addCommand("ktv", "[<Prefix>](唱K|([演]唱|K)(歌[曲]|<Music>))");

        parser.addSlot("QueryAction", Parser.compileLexicon("(查|查询|查看|搜[索]|看|睇|问[你])[[一]下]|查查看|看看|问问看|问问你"));
        parser.addCommand("weather", "[[<Prefix>]<QueryAction>][<City>][<Date>][的|嘅]天气[情况|信息|怎[么]样|[还]好吗|好不好|可好]");
        parser.addSlot("WeatherRainVerb", "下|降|落");
        parser.addSlot("WeatherRainVerbQuery", "下不下|降不降|落不落");
        parser.addSlot("WeatherRainType", "毛毛[细]|小|大|中|暴|雷|阵|雷阵");
        parser.addCommand(
                "weather.rain",
                "[[<Prefix>]<QueryAction>][<City>][<Date>][[的|嘅]天[气]][[会]<WeatherRainVerb>[<WeatherRainType>]雨吗|会不会<WeatherRainVerb>[<WeatherRainType>]雨[呢]|<WeatherRainVerbQuery>[<WeatherRainType>]雨[呢]]");
        parser.addSlot("WeatherWindVerb", "起|刮|吹|来");
        parser.addSlot("WeatherWindVerbQuery", "起不起|刮不刮|吹不吹|来不来");
        parser.addSlot("WeatherWindType", "阵|微|大|中|暴|台|飓|龙卷");
        parser.addCommand(
                "weather.wind",
                "[[<Prefix>]<QueryAction>][<City>][<Date>][[的|嘅]天[气]][[会]<WeatherWindVerb>风吗|会不会<WeatherWindVerb>风[呢]|<WeatherWindVerbQuery>风[呢]]");

        parser.addSlot("Stock", "[股票|证券]<StockName>|<StockName>[[的|嘅](股票|证券)]");
        parser.addSlot("StockTimeline", "分时");
        parser.addSlot("StockKlinePeriod", "(1|5|15|30|60)分钟|日|周|月|年");
        parser.addSlot("StockKline", "[<StockKlinePeriod>]K线");
        parser.addSlot("StockChart", "(<StockTimeline>|<StockKline>)[图]");
        parser.addCommand("stock", "[[<Prefix>](打开|查看)](股市|股票|沪市|上交所|深市|深交所|创业板)[大盘]");
        parser.addCommand("stock", "[[<Prefix>](打开|查看)]<Stock>[[的|嘅][股价|价格|行情|公司|<StockChart>][情况|信息]]");

        parser.addCommand("translate", "[<Prefix>][打开]翻译");
        parser.addCommand("merchandise", "[<Prefix>](网购|[上网](购物|买东西|买买买))");
        parser.addCommand("交费", "[[<Prefix>]交][手机|[电]话|水|电|[煤|液化|天然]气|[宽带]([上]网|网络)|[有线]电视|收视]费");
        parser.addCommand("website", "[<Prefix>](上网[冲浪]|(打开|浏览|看)网页)");

        parser.addCommand("播放记录", "[[<Prefix>](打开|[查]看)]播放记录");
        parser.addCommand("page", "[[<Prefix>](打开|(跳|翻|切)到)](第<Digit:1-200>|((上|下|前|后|第|最(前|后|末))[一])页)");

        mGrammar = parser.compileGrammar();
    }

    public void parse(String text) {
        long t = System.currentTimeMillis();
        Grammar.SearchResult r = mGrammar.search(text);
        t = System.currentTimeMillis() - t;
        if (r != null) {
            System.out.printf("Result id: %s\n", r.commandId);
            for (Map.Entry<String, String> param : r.params.entrySet())
                System.out.printf("  %s=%s\n", param.getKey(), param.getValue());
        }
        System.out.printf(" duration: %.3fs\n", t / 1000.);
        System.out.printf("==================================================\n");
    }

    public static void main(String[] args) {
        Example example = new Example();
        example.parse("我要看去年美国的电视剧");
        example.parse("麻烦帮我播放逻辑思维节目");
        example.parse("帮我查一下后天北京的天气");
    }
}
