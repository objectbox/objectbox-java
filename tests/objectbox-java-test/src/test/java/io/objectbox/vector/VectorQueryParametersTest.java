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

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import io.objectbox.annotation.VectorSearchLevel;
import io.objectbox.query.ObjectWithScore;
import io.objectbox.query.Query;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * Tests the {@link Query} parameter setters for nearest neighbor searches.
 */
public class VectorQueryParametersTest extends AbstractVectorTest {

    private static final String ALIAS = "vector";

    @Before
    public void putEntities() {
        putVectorEntities(100);
    }

    private Query<VectorEntity> queryNearest3() {
        return vectorBox().query(VectorEntity_.vector.nearestNeighbors(vector(42.2f), 3).alias(ALIAS)).build();
    }

    /**
     * Asserts the 3 nearest neighbors of {@link #queryNearest3()} and their distances: exact if rescored, else
     * estimated; the estimates of the similar test vectors may also miss some neighbors, so only checks the count.
     */
    private static void assertNearest3(String message, boolean rescored, List<ObjectWithScore<VectorEntity>> results) {
        if (rescored) {
            assertEquals(message, asList("42", "43", "41"), names(results));
        } else {
            assertEquals(message, 3, results.size());
        }
        assertExactDistances(rescored, 42.2f, results);
    }

    @Test
    public void setParameterSearchLevel_eachLevel_findsNearest() {
        try (Query<VectorEntity> query = queryNearest3()) {
            for (VectorSearchLevel level : VectorSearchLevel.values()) {
                if (level == VectorSearchLevel.DEFAULT) continue;
                // Levels 1-3 do not rescore: the order by the estimated distances may differ (similar vectors)
                boolean rescores = level.compareTo(VectorSearchLevel.L4_BALANCED) >= 0;
                query.setParameterSearchLevel(VectorEntity_.vector, level);
                assertNearest3(level.name(), rescores, query.findWithScores());
                query.setParameterSearchLevel(ALIAS, level);
                assertNearest3(level.name(), rescores, query.findWithScores());
            }
        }
    }

    @Test
    public void setParameterSearchLevel_default_throws() {
        try (Query<VectorEntity> query = queryNearest3()) {
            assertThrows(IllegalArgumentException.class,
                    () -> query.setParameterSearchLevel(VectorEntity_.vector, VectorSearchLevel.DEFAULT));
            assertThrows(IllegalArgumentException.class,
                    () -> query.setParameterSearchLevel(ALIAS, VectorSearchLevel.DEFAULT));
        }
    }

    @Test
    public void setParameter_searchParams_findsNearest() {
        try (Query<VectorEntity> query = queryNearest3()) {
            query.setParameterSearchLevel(VectorEntity_.vector, VectorSearchLevel.L2_VERY_FAST)
                    .setParameterOversampleFactor(VectorEntity_.vector, 2.0f)
                    .setParameterQuantizationOversample(VectorEntity_.vector, 1.5f)
                    .setParameterRescore(VectorEntity_.vector, true)
                    .setParameterRescoreFactor(VectorEntity_.vector, 2.0f)
                    .setParameterBruteForceMaxCandidates(VectorEntity_.vector, 100)
                    .setParameterFilteredSearchMaxNodes(VectorEntity_.vector, 1000);
            assertNearest3("rescored", true, query.findWithScores());

            query.setParameterOversampleFactor(ALIAS, 3.0f)
                    .setParameterQuantizationOversample(ALIAS, 0) // The index's
                    .setParameterRescore(ALIAS, true)
                    .setParameterRescoreFactor(ALIAS, 0) // The index's
                    .setParameterBruteForceMaxCandidates(ALIAS, 0) // The index's
                    .setParameterFilteredSearchMaxNodes(ALIAS, 0); // The index's
            assertNearest3("alias", true, query.findWithScores());

            query.setParameterRescore(VectorEntity_.vector, false);
            assertNearest3("not rescored", false, query.findWithScores());
        }
    }

    @Test
    public void setParameterRescoreFactor_turnsRescoringOn_lastCallWins() {
        try (Query<VectorEntity> query = queryNearest3()) {
            query.setParameterSearchLevel(VectorEntity_.vector, VectorSearchLevel.L1_FASTEST) // Does not rescore
                    .setParameterOversampleFactor(VectorEntity_.vector, 10);
            assertNearest3("level 1", false, query.findWithScores());

            query.setParameterRescoreFactor(VectorEntity_.vector, 10); // All explored candidates
            assertNearest3("factor", true, query.findWithScores());

            query.setParameterRescore(ALIAS, false);
            assertNearest3("rescore off after factor", false, query.findWithScores());

            query.setParameterRescoreFactor(ALIAS, 0); // The index's factor keeps the rescore setting
            assertNearest3("factor 0", false, query.findWithScores());
        }
    }

    @Test
    public void setParameter_searchParamsOutOfRange_throw() {
        try (Query<VectorEntity> query = queryNearest3()) {
            assertThrows(IllegalArgumentException.class,
                    () -> query.setParameterOversampleFactor(VectorEntity_.vector, 0.5f));
            assertThrows(IllegalArgumentException.class,
                    () -> query.setParameterQuantizationOversample(ALIAS, 0.5f));
            assertThrows(IllegalArgumentException.class,
                    () -> query.setParameterRescoreFactor(VectorEntity_.vector, 0.5f));
            assertThrows(IllegalArgumentException.class,
                    () -> query.setParameterBruteForceMaxCandidates(ALIAS, -1));
            assertThrows(IllegalArgumentException.class,
                    () -> query.setParameterFilteredSearchMaxNodes(VectorEntity_.vector, -1));
        }
    }

    @Test
    public void setParameterBruteForceMaxCandidates_belowCandidates_searchesGraph() {
        // At search level 1 without rescoring, only brute force finds exact distances
        try (Query<VectorEntity> query = vectorBox()
                .query(VectorEntity_.vector.nearestNeighbors(vector(42.2f), 3).alias(ALIAS)
                        .and(VectorEntity_.category.equal(2))) // Its index finds 10 candidates
                .build()) {
            query.setParameterSearchLevel(VectorEntity_.vector, VectorSearchLevel.L1_FASTEST);
            List<ObjectWithScore<VectorEntity>> results = query.findWithScores();
            assertEquals(asList("42", "52", "32"), names(results));
            assertExactDistances(true, 42.2f, results); // The index allows up to 10,000 candidates

            query.setParameterBruteForceMaxCandidates(VectorEntity_.vector, 9);
            assertExactDistances(false, 42.2f, query.findWithScores()); // Graph search with a filter

            query.setParameterBruteForceMaxCandidates(ALIAS, 10);
            assertExactDistances(true, 42.2f, query.findWithScores());

            query.setParameterBruteForceMaxCandidates(ALIAS, 9)
                    .setParameterFilteredSearchMaxNodes(ALIAS, 1000)
                    .setParameterFilteredSearchMaxNodes(VectorEntity_.vector, 1000);
            results = query.findWithScores();
            assertEquals(3, results.size());
            assertExactDistances(false, 42.2f, results);
        }
    }

    @Test
    public void setParameterSearchLevel_notNearestNeighborProperty_throws() {
        try (Query<VectorEntity> query = queryNearest3()) {
            assertThrows(IllegalArgumentException.class,
                    () -> query.setParameterSearchLevel(VectorEntity_.name, VectorSearchLevel.L4_BALANCED));
        }
    }

}
