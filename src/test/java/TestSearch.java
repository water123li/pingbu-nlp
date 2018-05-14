import pingbu.search.SearchLibrary;
import pingbu.search.SearchLoader;
import pingbu.search.SearchResult;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestSearch {

    private static final String CONN_STRING = "jdbc:sqlite:test_data/Video.db";

    public static void test() {
        System.out.print("Loading search...\n");
        long t = System.currentTimeMillis();
        final SearchLibrary library = new SearchLibrary();
        library.addField("name", SearchLoader.loadFuzzyIndexFromDB(CONN_STRING, "SELECT video_id, name FROM VideoNames ORDER BY video_id"));
        library.addField("category", SearchLoader.loadIndexFromDB(CONN_STRING, "SELECT id FROM Video WHERE category_id IN (SELECT id FROM Category WHERE name = {0}) ORDER BY id"));
        library.addField("type", SearchLoader.loadIndexFromDB(CONN_STRING, "SELECT video_id FROM VideoTypes WHERE type_id IN (SELECT id FROM Type WHERE name = {0}) ORDER BY video_id"));
        library.addField("release_year", SearchLoader.loadIndexFromDB(CONN_STRING, "SELECT id FROM Video WHERE release_year >= {0:int} AND release_year <= {1:int} ORDER BY id"));
        library.addField("language", SearchLoader.loadIndexFromDB(CONN_STRING, "SELECT video_id FROM VideoLanguages WHERE language_id IN (SELECT id FROM Language WHERE name = {0}) ORDER BY video_id"));
        library.addField("region", SearchLoader.loadIndexFromDB(CONN_STRING, "SELECT video_id FROM VideoRegions WHERE region_id IN (SELECT id FROM Region WHERE name = {0}) ORDER BY video_id"));
        library.addField("person", SearchLoader.loadIndexFromDB(CONN_STRING, "SELECT video_id FROM VideoStaffs WHERE artist_id IN (SELECT id FROM Artist WHERE name = {0}) OR role_id IN (SELECT id FROM Artist WHERE name = {0}) ORDER BY video_id"));
        library.addField("staff_job", SearchLoader.loadIndexFromDB(CONN_STRING, "SELECT video_id FROM VideoStaffs WHERE job_id IN (SELECT id FROM Job WHERE name = {0}) AND artist_id IN (SELECT id FROM Artist WHERE name = {0}) ORDER BY video_id"));
        library.addField("staff_role", SearchLoader.loadIndexFromDB(CONN_STRING, "SELECT video_id FROM VideoStaffs WHERE role_id IN (SELECT id FROM Role WHERE name = {0}) AND artist_id IN (SELECT id FROM Artist WHERE name = {0}) ORDER BY video_id"));
        library.addField("star", SearchLoader.loadIndexFromDB(CONN_STRING, "SELECT id FROM Video WHERE star >= {0:int} AND star <= {1:int} ORDER BY id"));
        library.addField("rate", SearchLoader.loadIndexFromDB(CONN_STRING, "SELECT id FROM Video WHERE rate >= {0:int} AND rate <= {1:int} ORDER BY id"));
        t = System.currentTimeMillis() - t;
        System.out.printf(" duration: %.3fs\n", t / 1000.);

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
        t = System.currentTimeMillis();
        final SearchResult[] srs = library.search(conditions,0,12);
        t = System.currentTimeMillis() - t;
        System.out.print("==================================================\n");
        System.out.printf("Conditions: %s\n", SearchLibrary.formatItem(conditions));
        final Map<Integer, String> rss = new HashMap<>();
        final List<String> ids = new ArrayList<>();
        for (final SearchResult r : srs)
            ids.add(Integer.toString(r.id));
        try (final Connection conn = DriverManager.getConnection(CONN_STRING);
             final Statement st = conn.createStatement();
             final ResultSet rs = st.executeQuery(String.format("SELECT Video.*, Category.name AS category FROM Video LEFT JOIN Category ON Category.id = Video.category_id WHERE Video.id IN (%s)", String.join(",", ids)))) {
            while (rs.next())
                rss.put(rs.getInt("id"), String.format("%s(%d) %s", rs.getString("name"), rs.getInt("release_year"), rs.getString("category")));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        for (final SearchResult r : srs)
            System.out.printf("  %.3f %s\n", r.score, rss.get(r.id));
        System.out.printf(" duration: %.3fs\n", t / 1000.);
    }
}
