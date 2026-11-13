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

import org.junit.Test;

import java.util.List;

import io.objectbox.HnswIndexParams;
import io.objectbox.HnswInitMode;
import io.objectbox.HnswInitResult;
import io.objectbox.annotation.VectorQuantization;
import io.objectbox.annotation.VectorSearchLevel;
import io.objectbox.model.HnswFlags;
import io.objectbox.query.Query;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * Tests the late init of an HNSW index (HnswFlags.LateInit in the model).
 */
public class HnswLateInitTest extends AbstractVectorTest {

    /**
     * The model's parameters: late init without dimensions.
     */
    private static HnswIndexParams lateInitParams() {
        return new HnswIndexParams(0).flags(HnswFlags.LateInit);
    }

    @Override
    protected HnswIndexParams hnswParams() {
        return lateInitParams();
    }

    private HnswInitResult initHnswIndex(HnswIndexParams params, HnswInitMode mode) {
        return store.initHnswIndex(VectorEntity_.vector, params, mode);
    }

    private List<String> findNearest3Names() {
        try (Query<VectorEntity> query = vectorBox()
                .query(VectorEntity_.vector.nearestNeighbors(vector(42.2f), 3))
                .build()) {
            return names(query.findWithScores());
        }
    }

    @Test
    public void lateInit_withoutInit_noDimensionsAndPutThrows() {
        assertEquals(0, store.getHnswIndexParams(VectorEntity_.vector).getDimensions());

        assertThrows(IllegalStateException.class, () -> vectorBox().put(new VectorEntity("1", 1, vector(1))));
        assertEquals(0, vectorBox().count()); // Reading works
    }

    @Test
    public void initHnswIndex_first_paramsSetAndPersisted() {
        HnswInitResult result = initHnswIndex(new HnswIndexParams(DIMENSIONS), HnswInitMode.KEEP_OBJECTS);
        assertTrue(result.isParamsChanged());
        assertFalse(result.isObjectsIndexed()); // No objects yet
        assertFalse(result.isObjectsRemoved());
        assertEquals(0, result.getObjectCount());

        HnswIndexParams params = store.getHnswIndexParams(VectorEntity_.vector);
        assertEquals(DIMENSIONS, params.getDimensions());
        assertEquals(HnswFlags.LateInit, params.getFlags());
        assertEquals(VectorQuantization.TURBO_QUANT_4, params.getQuantization()); // The default (32 dimensions)

        putVectorEntities(100);
        assertEquals(asList("42", "43", "41"), findNearest3Names());

        // The parameters in effect: nothing to do, also after the index was used
        assertTrue(initHnswIndex(new HnswIndexParams(DIMENSIONS), HnswInitMode.KEEP_OBJECTS).isUnchanged());

        // A later session uses the persisted parameters without init
        reopenStore(lateInitParams());
        assertEquals(DIMENSIONS, store.getHnswIndexParams(VectorEntity_.vector).getDimensions());
        assertEquals(asList("42", "43", "41"), findNearest3Names());
    }

    @Test
    public void initHnswIndex_otherParams_indexesOrRemovesObjects() {
        initHnswIndex(new HnswIndexParams(DIMENSIONS), HnswInitMode.KEEP_OBJECTS);
        putVectorEntities(100);

        // Re-indexes for another indexing search count
        reopenStore(lateInitParams());
        HnswIndexParams params = new HnswIndexParams(DIMENSIONS).indexingSearchCount(64);
        HnswInitResult result = initHnswIndex(params, HnswInitMode.KEEP_OBJECTS);
        assertTrue(result.isParamsChanged());
        assertTrue(result.isObjectsIndexed());
        assertEquals(100, result.getObjectCount());
        assertEquals(64, store.getHnswIndexParams(VectorEntity_.vector).getIndexingSearchCount());
        assertEquals(asList("42", "43", "41"), findNearest3Names());

        // Another search level does not require re-indexing: keeps the objects
        reopenStore(lateInitParams());
        params.searchLevel(VectorSearchLevel.L6_QUALITY);
        result = initHnswIndex(params, HnswInitMode.REMOVE_ALL_OBJECTS_ON_REINDEX);
        assertTrue(result.isParamsChanged());
        assertFalse(result.isObjectsIndexed());
        assertFalse(result.isObjectsRemoved());
        assertEquals(100, vectorBox().count());

        // Other dimensions require re-indexing: removes the objects
        reopenStore(lateInitParams());
        result = initHnswIndex(new HnswIndexParams(16), HnswInitMode.REMOVE_ALL_OBJECTS_ON_REINDEX);
        assertTrue(result.isParamsChanged());
        assertTrue(result.isObjectsRemoved());
        assertEquals(100, result.getObjectCount());
        assertEquals(0, vectorBox().count());

        // Always removes, also with the parameters in effect and after the index was used
        putVectorEntities(10);
        result = initHnswIndex(new HnswIndexParams(16), HnswInitMode.REMOVE_ALL_OBJECTS_ALWAYS);
        assertFalse(result.isParamsChanged());
        assertTrue(result.isObjectsRemoved());
        assertEquals(10, result.getObjectCount());
        assertEquals(0, vectorBox().count());
    }

    @Test
    public void initHnswIndex_otherParamsAfterIndexUsed_throws() {
        initHnswIndex(new HnswIndexParams(DIMENSIONS), HnswInitMode.KEEP_OBJECTS);
        putVectorEntities(1);

        assertThrows(IllegalStateException.class,
                () -> initHnswIndex(new HnswIndexParams(16), HnswInitMode.KEEP_OBJECTS));
    }

    @Test
    public void initHnswIndex_invalidArguments_throw() {
        assertThrows(IllegalArgumentException.class,
                () -> initHnswIndex(new HnswIndexParams(0), HnswInitMode.KEEP_OBJECTS));
        assertThrows(IllegalArgumentException.class,
                () -> initHnswIndex(new HnswIndexParams(DIMENSIONS).quantizationOversample(0.5f),
                        HnswInitMode.KEEP_OBJECTS));
        assertThrows(IllegalArgumentException.class,
                () -> store.initHnswIndex(VectorEntity_.name, new HnswIndexParams(DIMENSIONS),
                        HnswInitMode.KEEP_OBJECTS));

        // No late init
        reopenStore(new HnswIndexParams(DIMENSIONS));
        assertThrows(IllegalArgumentException.class,
                () -> initHnswIndex(new HnswIndexParams(DIMENSIONS), HnswInitMode.KEEP_OBJECTS));
    }

}
