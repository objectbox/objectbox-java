/*
 * Copyright 2024 ObjectBox Ltd. All rights reserved.
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

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

import javax.annotation.Nullable;

import io.objectbox.annotation.IndexType;


import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Sets a custom error output stream to assert log messages of {@link BoxStore}.
 */
public class BoxStoreLogTest extends AbstractObjectBoxTest {

    private ByteArrayOutputStream errOutput;

    @Override
    protected BoxStoreBuilder createBoxStoreBuilder(@Nullable IndexType simpleStringIndexType) {
        BoxStoreBuilder builder = super.createBoxStoreBuilder(simpleStringIndexType);
        errOutput = new ByteArrayOutputStream();
        builder.setErrorOutput(new PrintStream(errOutput));
        return builder;
    }

    @Test
    public void close_activeThreadPool_printsError() throws UnsupportedEncodingException {
        // Submit two threads, one to the internal pool, that run longer
        // than BoxStore.close waits on the pool to terminate
        new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {
            }
        }).start();
        store.internalScheduleThread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignored) {
            }
        });
        // Close store to trigger thread pool shutdown, store waits 1 second for shutdown
        store.close();

        String errOutput = this.errOutput.toString("UTF-8");
        assertTrue(errOutput.contains("ObjectBox thread pool not terminated in time"));
        assertTrue(errOutput.contains("=== BEGIN OF DUMP ==="));
        assertTrue(errOutput.contains("=== END OF DUMP ==="));
        // Check that only pool threads or threads with stack traces that contain the objectbox package are printed
        String[] lines = errOutput.split("\n");
        String checkStackTrace = null;
        for (String line : lines) {
            if (line.startsWith("Thread:")) {
                if (checkStackTrace != null) {
                    fail("Expected stack trace to contain class in objectbox package");
                }
                if (!line.contains("ObjectBox-")) {
                    checkStackTrace = line;
                }
            } else if (checkStackTrace != null) {
                if (line.contains("objectbox")) {
                    checkStackTrace = null;
                }
            }
        }
    }

}