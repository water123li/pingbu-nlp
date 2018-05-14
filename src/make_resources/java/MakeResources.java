import pingbu.common.FileStorage;
import pingbu.common.JarStorage;
import pingbu.common.Pinyin;
import pingbu.nlp.Grammar;
import pingbu.nlp.NlpFile;
import pingbu.search.IndexDatabase;
import pingbu.search.Search;
import pingbu.search.SearchDatabase;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.*;
import java.util.*;

public class MakeResources {
    public static void main(String[] args) {
        System.out.printf("Making resources...\n");
        long t = System.currentTimeMillis();
        try {
            Pinyin.createModal(new JarStorage(MakeResources.class.getClassLoader()));
            Pinyin.saveModal(new FileStorage("src/main/resources"));
            System.out.printf("Success.\n");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            t = System.currentTimeMillis() - t;
            System.out.printf(" duration: %.3fs\n", t / 1000.);
        }
    }
}
