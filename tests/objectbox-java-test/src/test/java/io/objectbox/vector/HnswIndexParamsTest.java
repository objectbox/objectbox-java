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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import io.objectbox.HnswIndexParams;
import io.objectbox.annotation.VectorBuildLevel;
import io.objectbox.annotation.VectorDistanceType;
import io.objectbox.annotation.VectorQuantization;
import io.objectbox.annotation.VectorSearchLevel;
import io.objectbox.model.HnswFlags;
import io.objectbox.query.ObjectWithScore;
import io.objectbox.query.Query;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class HnswIndexParamsTest extends AbstractVectorTest {

    @Test
    public void levels_ordinal_isLevel() {
        // HnswIndexParams and queries pass the ordinal as the level number
        assertEquals(0, VectorSearchLevel.DEFAULT.ordinal());
        assertEquals(1, VectorSearchLevel.L1_FASTEST.ordinal());
        assertEquals(4, VectorSearchLevel.L4_BALANCED.ordinal());
        assertEquals(10, VectorSearchLevel.L10_QUALITY_MAX.ordinal());
        assertEquals(0, VectorBuildLevel.DEFAULT.ordinal());
        assertEquals(1, VectorBuildLevel.L1_FASTEST.ordinal());
        assertEquals(4, VectorBuildLevel.L4_BALANCED.ordinal());
        assertEquals(10, VectorBuildLevel.L10_QUALITY_MAX.ordinal());
    }

    @Test
    public void hnswParams_allSet_findsNearestNeighbors() {
        reopenStore(new HnswIndexParams(DIMENSIONS)
                .distanceType(VectorDistanceType.EUCLIDEAN)
                .reparationBacklinkProbability(0.9f)
                .vectorCacheHintSizeKb(100 * 1024)
                .quantization(VectorQuantization.SCALAR_QUANT_8)
                .quantizationOversample(2.0f)
                .quantizationRescoreFactor(1.5f)
                .searchLevel(VectorSearchLevel.L6_QUALITY)
                .buildLevel(VectorBuildLevel.L2_VERY_FAST)
                .neighborsPerNode(16) // Takes precedence over the build level
                .bruteForceMaxCandidates(100)
                .filteredSearchMaxNodes(1000)
                .randomSeed(1));
        putVectorEntities(100);

        try (Query<VectorEntity> query = vectorBox()
                .query(VectorEntity_.vector.nearestNeighbors(vector(42.2f), 3))
                .build()) {
            List<ObjectWithScore<VectorEntity>> results = query.findWithScores();
            assertEquals(3, results.size());
            assertEquals("42", results.get(0).get().getName());
            assertEquals("43", results.get(1).get().getName());
            assertEquals("41", results.get(2).get().getName());
        }
    }

    @Test
    public void getHnswIndexParams_defaults() {
        HnswIndexParams params = store.getHnswIndexParams(VectorEntity_.vector);
        assertEquals(DIMENSIONS, params.getDimensions());
        assertEquals(30, params.getNeighborsPerNode());
        assertEquals(0, params.getIndexingSearchCount()); // Not set: the default (currently 128)
        assertEquals(0, params.getFlags());
        assertEquals(VectorDistanceType.EUCLIDEAN, params.getDistanceType());
        assertEquals(1.0f, params.getReparationBacklinkProbability(), 0);
        assertEquals(0, params.getVectorCacheHintSizeKb());
        assertEquals(VectorQuantization.TURBO_QUANT_4, params.getQuantization()); // At least 32 dimensions
        assertEquals(0, params.getQuantizationOversample(), 0);
        assertEquals(0, params.getQuantizationRescoreFactor(), 0);
        assertEquals(VectorSearchLevel.DEFAULT, params.getSearchLevel());
        assertEquals(VectorBuildLevel.DEFAULT, params.getBuildLevel());
        assertEquals(0, params.getBruteForceMaxCandidates());
        assertEquals(0, params.getFilteredSearchMaxNodes());
        assertEquals(0, params.getRandomSeed()); // A random seed
    }

    @Test
    public void getHnswIndexParams_allSet_returnsThem() {
        reopenStore(new HnswIndexParams(DIMENSIONS)
                .flags(HnswFlags.VectorCacheSimdPaddingOff)
                .distanceType(VectorDistanceType.COSINE)
                .reparationBacklinkProbability(0.9f)
                .vectorCacheHintSizeKb(100 * 1024)
                .quantization(VectorQuantization.SCALAR_QUANT_8)
                .quantizationOversample(2.0f)
                .quantizationRescoreFactor(1.5f)
                .searchLevel(VectorSearchLevel.L6_QUALITY)
                .buildLevel(VectorBuildLevel.L2_VERY_FAST) // M 20 / efConstruction 64
                .neighborsPerNode(16) // Takes precedence over the build level
                .bruteForceMaxCandidates(100)
                .filteredSearchMaxNodes(1000)
                .randomSeed(0x123456789ABCDEF0L)); // More than 32 bits

        HnswIndexParams params = store.getHnswIndexParams(VectorEntity_.vector);
        assertEquals(DIMENSIONS, params.getDimensions());
        assertEquals(16, params.getNeighborsPerNode());
        assertEquals(64, params.getIndexingSearchCount());
        assertEquals(HnswFlags.VectorCacheSimdPaddingOff, params.getFlags());
        assertEquals(VectorDistanceType.COSINE, params.getDistanceType());
        assertEquals(0.9f, params.getReparationBacklinkProbability(), 0);
        assertEquals(100 * 1024, params.getVectorCacheHintSizeKb());
        assertEquals(VectorQuantization.SCALAR_QUANT_8, params.getQuantization());
        assertEquals(2.0f, params.getQuantizationOversample(), 0);
        assertEquals(1.5f, params.getQuantizationRescoreFactor(), 0);
        assertEquals(VectorSearchLevel.L6_QUALITY, params.getSearchLevel());
        assertEquals(VectorBuildLevel.DEFAULT, params.getBuildLevel()); // Resolved, not stored
        assertEquals(100, params.getBruteForceMaxCandidates());
        assertEquals(1000, params.getFilteredSearchMaxNodes());
        assertEquals(0x123456789ABCDEF0L, params.getRandomSeed());
    }

    @Test
    public void randomSeed_sameObjectsInSameOrder_sameResults() {
        // A new store with the same seed and objects builds the same graph: approximate results (here: random vectors
        // and the fastest search level) are the same
        HnswIndexParams seeded = new HnswIndexParams(DIMENSIONS).randomSeed(42);
        reopenStore(seeded);
        List<String> results = putRandomVectorsAndSearch();
        store.close();
        store.deleteAllFiles();
        store = createVectorStore(seeded);
        assertEquals(results, putRandomVectorsAndSearch());
    }

    private List<String> putRandomVectorsAndSearch() {
        Random random = new Random(1);
        List<VectorEntity> entities = new ArrayList<>();
        for (int i = 1; i <= 1000; i++) {
            float[] vector = new float[DIMENSIONS];
            for (int d = 0; d < DIMENSIONS; d++) {
                vector[d] = random.nextFloat();
            }
            entities.add(new VectorEntity(String.valueOf(i), i % 10, vector));
        }
        vectorBox().put(entities);

        float[] queryVector = new float[DIMENSIONS];
        for (int d = 0; d < DIMENSIONS; d++) {
            queryVector[d] = random.nextFloat();
        }
        try (Query<VectorEntity> query = vectorBox()
                .query(VectorEntity_.vector.nearestNeighbors(queryVector, 10)
                        .searchLevel(VectorSearchLevel.L1_FASTEST))
                .build()) {
            List<ObjectWithScore<VectorEntity>> results = query.findWithScores();
            assertEquals(10, results.size());
            return names(results);
        }
    }

    @Test
    public void getHnswIndexParams_noHnswIndex_throws() {
        assertThrows(IllegalArgumentException.class, () -> store.getHnswIndexParams(VectorEntity_.name));
    }

    @Test
    public void hnswParams_quantizationOversampleBelow1_throws() {
        store.close();
        store.deleteAllFiles();
        store = null; // Do not clean up again
        HnswIndexParams params = new HnswIndexParams(DIMENSIONS).quantizationOversample(0.5f);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> createVectorStore(params));
        assertTrue(ex.getMessage(), ex.getMessage().contains("oversample"));
    }

}
