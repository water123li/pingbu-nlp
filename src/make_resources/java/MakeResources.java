import pingbu.common.FileStorage;
import pingbu.common.JarStorage;
import pingbu.common.Pinyin;

import java.io.IOException;

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
