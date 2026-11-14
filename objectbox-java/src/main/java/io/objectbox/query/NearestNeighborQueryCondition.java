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

package io.objectbox.query;

import io.objectbox.Property;
import io.objectbox.annotation.HnswIndex;
import io.objectbox.annotation.VectorSearchLevel;

/**
 * A nearest neighbor condition (vector search) created by {@link Property#nearestNeighbors(float[], int)}.
 * <p>
 * Optionally, set its initial search parameters, e.g. the {@link #searchLevel(VectorSearchLevel) search level}; a built
 * query can change them, e.g. with {@link Query#setParameterSearchLevel(Property, VectorSearchLevel)}.
 */
public interface NearestNeighborQueryCondition<T> extends PropertyQueryCondition<T> {

    /**
     * Sets the {@link VectorSearchLevel search level}, the preferred way to trade search speed for quality (recall).
     * Without it, the search uses the level of the index ({@link HnswIndex#searchLevel()}).
     * <p>
     * The other search parameters of this condition are applied after the level, regardless of the call order (the
     * level replaces the oversample and rescore parameters).
     *
     * @param level The search level; not {@link VectorSearchLevel#DEFAULT}.
     */
    NearestNeighborQueryCondition<T> searchLevel(VectorSearchLevel level);

    /**
     * For power users (prefer the {@link #searchLevel(VectorSearchLevel) search level}): sets how many candidates the
     * search explores per result, independent of quantization (at least 1).
     *
     * @see Query#setParameterOversampleFactor(Property, float)
     */
    NearestNeighborQueryCondition<T> oversampleFactor(float factor);

    /**
     * For power users (prefer the {@link #searchLevel(VectorSearchLevel) search level}): with quantization, rescores
     * this factor times maxResultCount of the explored candidates (those with the nearest estimates; at least 1, capped
     * at the explored candidates) with the full-precision vectors. This also turns rescoring on, e.g. for the search
     * levels that do not rescore otherwise (1-3).
     *
     * @see Query#setParameterRescoreFactor(Property, float)
     */
    NearestNeighborQueryCondition<T> rescoreFactor(float factor);

    /**
     * For a search with other conditions: finds the nearest neighbors by brute force among at most this many candidates
     * of the lookups of the other conditions; overrides the value of the index
     * ({@link HnswIndex#bruteForceMaxCandidates()}), 0 uses it.
     *
     * @see Query#setParameterBruteForceMaxCandidates(Property, int)
     */
    NearestNeighborQueryCondition<T> bruteForceMaxCandidates(int maxCandidates);

    /**
     * For a search with other conditions: the graph search explores at most this many nodes; overrides the value of the
     * index ({@link HnswIndex#filteredSearchMaxNodes()}), 0 uses it.
     *
     * @see Query#setParameterFilteredSearchMaxNodes(Property, int)
     */
    NearestNeighborQueryCondition<T> filteredSearchMaxNodes(int maxNodes);

    /**
     * Assigns an alias to this condition that can later be used with the {@link Query} setParameter methods.
     */
    @Override
    NearestNeighborQueryCondition<T> alias(String name);

}
