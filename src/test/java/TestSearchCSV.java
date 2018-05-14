import pingbu.common.JarStorage;
import pingbu.search.SearchLibrary;
import pingbu.search.SearchLibraryWithData;
import pingbu.search.SearchLoader;
import pingbu.search.SearchResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestSearchCSV {

    public static void test() {
        System.out.print("Loading search...\n");
        long t = System.currentTimeMillis();
        final SearchLibraryWithData library = SearchLoader.loadSearchLibraryFromCSV(new JarStorage(TestSearchCSV.class.getClassLoader()), "Book.csv");
        t = System.currentTimeMillis() - t;
        System.out.printf(" duration: %.3fs\n", t / 1000.);

        final Map<String, String> conditions = new HashMap<String, String>() {
            private static final long serialVersionUID = 1L;

            {
                put("Grade", "8");
                put("Subject", "物理");
            }
        };
        t = System.currentTimeMillis();
        final SearchResult[] srs = library.search(conditions, 0, 12);
        t = System.currentTimeMillis() - t;
        System.out.print("==================================================\n");
        System.out.printf("Conditions: %s\n", SearchLibrary.formatItem(conditions));
        final Map<Integer, String> rss = new HashMap<>();
        final List<String> ids = new ArrayList<>();
        for (final SearchResult r : srs)
            System.out.printf("  %.3f %s\n", r.score, SearchLibrary.formatItem(library.getItem(r.id)));
        System.out.printf(" duration: %.3fs\n", t / 1000.);
    }
}
