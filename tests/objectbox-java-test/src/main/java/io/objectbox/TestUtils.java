package io.objectbox;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.greenrobot.essentials.io.FileUtils;
import org.greenrobot.essentials.io.IoUtils;

public class TestUtils {
    public static String loadFile(String filename) {
        String json;
        InputStream in = TestUtils.class.getResourceAsStream("/" + filename);
        try {
            if (in != null) {
                Reader reader = new InputStreamReader(in, "UTF-8");
                json = IoUtils.readAllCharsAndClose(reader);

            } else {
                String pathname = "src/main/resources/" + filename;
                File file = new File(pathname);
                if (!file.exists()) {
                    file = new File("lib-test-java/" + pathname);
                }
                json = FileUtils.readUtf8(file);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return json;
    }
}