/*
 * Copyright 2026 ObjectBox Ltd. <https://objectbox.io>
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

package io.objectbox.vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.objectbox.AbstractObjectBoxTest;
import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.HnswIndexParams;

/**
 * Tests with {@link VectorEntity}, which has an HNSW index on its vector property.
 */
public abstract class AbstractVectorTest extends AbstractObjectBoxTest {

    /**
     * The vectors have at least 32 dimensions, so the index uses quantization by default.
     */
    protected static final int DIMENSIONS = 32;

    /**
     * The HNSW index parameters of the store a test starts with.
     */
    protected HnswIndexParams hnswParams() {
        return new HnswIndexParams(DIMENSIONS);
    }

    @Override
    protected BoxStore createBoxStore() {
        return createVectorStore(hnswParams());
    }

    protected BoxStore createVectorStore(HnswIndexParams hnswParams) {
        return MyObjectBox.builder(hnswParams).directory(boxStoreDir).build();
    }

    /**
     * Closes the store and opens it again with the given HNSW index parameters (keeps the files).
     */
    protected void reopenStore(HnswIndexParams hnswParams) {
        store.close();
        store = createVectorStore(hnswParams);
    }

    protected Box<VectorEntity> vectorBox() {
        return store.boxFor(VectorEntity.class);
    }

    /**
     * A vector with all elements set to the given value; the nearest neighbors of {@code vector(x)} are the vectors
     * with the values closest to x.
     */
    protected static float[] vector(float value) {
        float[] vector = new float[DIMENSIONS];
        Arrays.fill(vector, value);
        return vector;
    }

    /**
     * Puts objects with the vectors 1 to {@code count} (see {@link #vector(float)}), named by their value (e.g. "1");
     * their category is the value modulo 10.
     */
    protected List<VectorEntity> putVectorEntities(int count) {
        List<VectorEntity> entities = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            entities.add(new VectorEntity(String.valueOf(i), i % 10, vector(i)));
        }
        vectorBox().put(entities);
        return entities;
    }
}
