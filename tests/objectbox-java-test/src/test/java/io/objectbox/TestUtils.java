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

import org.greenrobot.essentials.io.IoUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.Serializable;

public class TestUtils {

    public static String loadFile(String filename) {
        try {
            InputStream in = openInputStream("/" + filename);
            Reader reader = new InputStreamReader(in, "UTF-8");
            return IoUtils.readAllCharsAndClose(reader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static InputStream openInputStream(String filename) throws FileNotFoundException {
        InputStream in = TestUtils.class.getResourceAsStream("/" + filename);
        if (in == null) {
            String pathname = "src/main/resources/" + filename;
            File file = new File(pathname);
            if (!file.exists()) {
                file = new File("lib-test-java/" + pathname);
            }
            in = new FileInputStream(file);
        }
        return in;
    }

    @SuppressWarnings("unchecked")
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