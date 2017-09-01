/*
 * Copyright 2017 ObjectBox Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.objectbox;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.Serializable;

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

    public static <T extends Serializable> T serializeDeserialize(T entity) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bytesOut);
        out.writeObject(entity);
        out.close();
        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytesOut.toByteArray()));
        Object entityDeserialized = in.readObject();
        in.close();
        return (T) entityDeserialized;
    }
}