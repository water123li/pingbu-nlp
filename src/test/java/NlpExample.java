import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pingbu.common.JarStorage;
import pingbu.common.Pinyin;
import pingbu.nlp.Grammar;
import pingbu.nlp.NlpFile;
import pingbu.search.IndexDatabase;
import pingbu.search.Search;
import pingbu.search.SearchDatabase;

public class NlpExample {

    private static final String CONN_STRING = "jdbc:sqlite:test_data/Video.db";

    private final Grammar mGrammar;
    private final Search mSearch;

    public NlpExample() {
        System.out.printf("Loading grammar...\n");
        long t = System.currentTimeMillis();
        mGrammar = NlpFile.loadGrammar(new JarStorage(NlpExample.class.getClassLoader()), "Grammar.txt");
        t = System.currentTimeMillis() - t;
        System.out.printf(" duration: %.3fs\n", t / 1000.);

        System.out.printf("Loading search...\n");
        t = System.currentTimeMillis();
        mSearch = new Search();
        mSearch.addField("name", SearchDatabase.loadSearchIndex(CONN_STRING, "SELECT video_id, name FROM VideoNames ORDER BY video_id"));
        mSearch.addField("category", new IndexDatabase(CONN_STRING, "SELECT id FROM Video WHERE category_id IN (SELECT id FROM Category WHERE name = {0}) ORDER BY id"));
        mSearch.addField("type", new IndexDatabase(CONN_STRING, "SELECT video_id FROM VideoTypes WHERE type_id IN (SELECT id FROM Type WHERE name = {0}) ORDER BY video_id"));
        mSearch.addField("release_year", new IndexDatabase(CONN_STRING, "SELECT id FROM Video WHERE release_year >= {0:int} AND release_year <= {1:int} ORDER BY id"));
        mSearch.addField("language", new IndexDatabase(CONN_STRING, "SELECT video_id FROM VideoLanguages WHERE language_id IN (SELECT id FROM Language WHERE name = {0}) ORDER BY video_id"));
        mSearch.addField("region", new IndexDatabase(CONN_STRING, "SELECT video_id FROM VideoRegions WHERE region_id IN (SELECT id FROM Region WHERE name = {0}) ORDER BY video_id"));
        mSearch.addField("person", new IndexDatabase(CONN_STRING, "SELECT video_id FROM VideoStaffs WHERE artist_id IN (SELECT id FROM Artist WHERE name = {0}) OR role_id IN (SELECT id FROM Artist WHERE name = {0}) ORDER BY video_id"));
        mSearch.addField("staff_job", new IndexDatabase(CONN_STRING, "SELECT video_id FROM VideoStaffs WHERE job_id IN (SELECT id FROM Job WHERE name = {0}) AND artist_id IN (SELECT id FROM Artist WHERE name = {0}) ORDER BY video_id"));
        mSearch.addField("staff_role", new IndexDatabase(CONN_STRING, "SELECT video_id FROM VideoStaffs WHERE role_id IN (SELECT id FROM Role WHERE name = {0}) AND artist_id IN (SELECT id FROM Artist WHERE name = {0}) ORDER BY video_id"));
        mSearch.addField("star", new IndexDatabase(CONN_STRING, "SELECT id FROM Video WHERE star >= {0:int} AND star <= {1:int} ORDER BY id"));
        mSearch.addField("rate", new IndexDatabase(CONN_STRING, "SELECT id FROM Video WHERE rate >= {0:int} AND rate <= {1:int} ORDER BY id"));
        t = System.currentTimeMillis() - t;
        System.out.printf(" duration: %.3fs\n", t / 1000.);
    }

    public void parse(String text) {
        long t = System.currentTimeMillis();
        final Grammar.SearchResult r = mGrammar.search(text);
        t = System.currentTimeMillis() - t;
        System.out.printf("==================================================\n");
        System.out.printf("Text: %s\n", text);
        if (r != null) {
            System.out.printf("Result: %.3f\n", r.score);
            for (final Map.Entry<String, String> param : r.params.entrySet())
                System.out.printf("  %s=%s\n", param.getKey(), param.getValue());
        } else {
            System.out.printf("Result: null\n");
        }
        System.out.printf(" duration: %.3fs\n", t / 1000.);
    }

    public static void main(String[] args) {
        // Logger.setLogger(new ConsoleLogger(true));
        Pinyin.init();

        final NlpExample example = new NlpExample();
        example.parse("帮我查一下后天北京的天气");
        example.parse("回看中央2台昨晚8点多钟的节目");
        example.parse("麻烦帮我播放逻辑思维节目");
        example.parse("我要点播闯关节目");
        example.parse("我享看2010年代的美国科幻片移动迷宫");

        final Map<String, String> conditions = new HashMap<String, String>() {
            private static final long serialVersionUID = 1L;

            {
                put("name", "移动迷宫");
                put("category", "电影");
                put("type", "科幻");
                put("release_year", "2010,2019");
                put("region", "美国");
            }
        };
        long t = System.currentTimeMillis();
        final Collection<Search.Result> srs = example.mSearch.search(conditions, 12);
        t = System.currentTimeMillis() - t;
        System.out.printf("==================================================\n");
        System.out.printf("Conditions: %s\n", Search.formatItem(conditions));
        final Map<Integer, String> rss = new HashMap<Integer, String>();
        final List<String> ids = new ArrayList<String>();
        for (final Search.Result r : srs)
            ids.add(Integer.toString(r.id));
        try (final Connection conn = DriverManager.getConnection(CONN_STRING);
             final Statement st = conn.createStatement();
             final ResultSet rs = st.executeQuery(String.format("SELECT Video.*, Category.name AS category FROM Video LEFT JOIN Category ON Category.id = Video.category_id WHERE Video.id IN (%s)", String.join(",", ids)))) {
            while (rs.next())
                rss.put(rs.getInt("id"), String.format("%s(%d) %s", rs.getString("name"), rs.getInt("release_year"), rs.getString("category")));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        for (final Search.Result r : srs)
            System.out.printf("  %.3f %s\n", r.score, rss.get(r.id));
        System.out.printf(" duration: %.3fs\n", t / 1000.);
    }
}
