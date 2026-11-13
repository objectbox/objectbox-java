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
import io.objectbox.query.NearestNeighborQueryCondition;
import io.objectbox.query.ObjectWithScore;
import io.objectbox.query.Query;
import io.objectbox.query.QueryBuilder;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * Tests the search parameters of a nearest neighbor condition, set by {@link NearestNeighborQueryCondition} or the
 * vector search modifiers of {@link QueryBuilder}.
 */
public class VectorSearchModifiersTest extends AbstractVectorTest {

    @Before
    public void putEntities() {
        putVectorEntities(100);
    }

    /**
     * Without rescoring (e.g. search level 1), the results have estimated distances; rescoring all explored candidates
     * finds the exact nearest neighbors with their exact distances.
     */
    private static void assertRescoredNearest3(Query<VectorEntity> query) {
        List<ObjectWithScore<VectorEntity>> results = query.findWithScores();
        assertEquals(asList("42", "43", "41"), names(results));
        assertExactDistances(true, 42.2f, results);
    }

    @Test
    public void condition_searchLevel_findsNearest() {
        try (Query<VectorEntity> query = vectorBox()
                .query(VectorEntity_.vector.nearestNeighbors(vector(42.2f), 3)
                        .searchLevel(VectorSearchLevel.L6_QUALITY))
                .build()) {
            assertRescoredNearest3(query);
        }
    }

    @Test
    public void condition_searchParams_rescore() {
        // Set in a different order than applied: the search level would replace the others
        try (Query<VectorEntity> query = vectorBox()
                .query(VectorEntity_.vector.nearestNeighbors(vector(42.2f), 3)
                        .rescoreFactor(10)
                        .oversampleFactor(10)
                        .bruteForceMaxCandidates(0)
                        .filteredSearchMaxNodes(0)
                        .searchLevel(VectorSearchLevel.L1_FASTEST))
                .build()) {
            assertRescoredNearest3(query);
        }
    }

    @Test
    public void condition_aliasBeforeAndAfterSearchParams_setParameterWorks() {
        try (Query<VectorEntity> query = vectorBox()
                .query(VectorEntity_.vector.nearestNeighbors(vector(42.2f), 3)
                        .searchLevel(VectorSearchLevel.L1_FASTEST)
                        .alias("vector")
                        .oversampleFactor(10))
                .build()) {
            // Like the condition's rescoreFactor(), the setter turns on rescoring (off at level 1)
            query.setParameterRescoreFactor("vector", 10);
            assertRescoredNearest3(query);
        }
    }

    @Test
    public void condition_bruteForceMaxCandidates_belowCandidates_searchesGraph() {
        // At search level 1 without rescoring, only brute force finds exact distances;
        // the index of category finds 10 candidates
        try (Query<VectorEntity> query = vectorBox()
                .query(VectorEntity_.vector.nearestNeighbors(vector(42.2f), 3)
                        .searchLevel(VectorSearchLevel.L1_FASTEST)
                        .bruteForceMaxCandidates(9)
                        .filteredSearchMaxNodes(1000)
                        .and(VectorEntity_.category.equal(2)))
                .build()) {
            List<ObjectWithScore<VectorEntity>> results = query.findWithScores();
            assertEquals(3, results.size());
            assertExactDistances(false, 42.2f, results);
        }
        try (Query<VectorEntity> query = vectorBox()
                .query(VectorEntity_.vector.nearestNeighbors(vector(42.2f), 3)
                        .searchLevel(VectorSearchLevel.L1_FASTEST)
                        .bruteForceMaxCandidates(10)
                        .and(VectorEntity_.category.equal(2)))
                .build()) {
            assertExactDistances(true, 42.2f, query.findWithScores());
        }
    }

    @Test
    public void condition_invalidSearchParams_throw() {
        NearestNeighborQueryCondition<VectorEntity> condition = VectorEntity_.vector.nearestNeighbors(vector(42.2f), 3);
        assertThrows(IllegalArgumentException.class, () -> condition.searchLevel(VectorSearchLevel.DEFAULT));

        condition.oversampleFactor(0.5f);
        assertThrows(IllegalArgumentException.class, () -> vectorBox().query(condition).build());
    }

    @Test
    public void queryBuilder_modifiers_rescore() {
        try (Query<VectorEntity> query = vectorBox().query()
                .nearestNeighbors(VectorEntity_.vector, vector(42.2f), 3)
                .vectorSearchLevel(VectorSearchLevel.L1_FASTEST)
                .vectorOversampleFactor(10)
                .parameterAlias("vector") // Does not affect the modifiers
                .vectorRescoreFactor(10)
                .vectorBruteForceMaxCandidates(0)
                .vectorFilteredSearchMaxNodes(0)
                .build()) {
            assertRescoredNearest3(query);
        }
    }

    @Test
    public void queryBuilder_bruteForceMaxCandidates_belowCandidates_searchesGraph() {
        // See condition_bruteForceMaxCandidates_belowCandidates_searchesGraph()
        try (Query<VectorEntity> query = vectorBox().query()
                .nearestNeighbors(VectorEntity_.vector, vector(42.2f), 3)
                .vectorSearchLevel(VectorSearchLevel.L1_FASTEST)
                .vectorBruteForceMaxCandidates(9)
                .vectorFilteredSearchMaxNodes(1000)
                .equal(VectorEntity_.category, 2)
                .build()) {
            List<ObjectWithScore<VectorEntity>> results = query.findWithScores();
            assertEquals(3, results.size());
            assertExactDistances(false, 42.2f, results);
        }
    }

    @Test
    public void queryBuilder_modifierNotAfterNearestNeighbors_throws() {
        QueryBuilder<VectorEntity> builder = vectorBox().query();
        try {
            // No previous condition
            assertThrows(IllegalStateException.class, () -> builder.vectorSearchLevel(VectorSearchLevel.L4_BALANCED));

            // Other condition after the nearest neighbor condition
            builder.nearestNeighbors(VectorEntity_.vector, vector(42.2f), 3)
                    .equal(VectorEntity_.category, 2);
            assertThrows(IllegalStateException.class, () -> builder.vectorOversampleFactor(2));
        } finally {
            builder.close();
        }
    }

}
