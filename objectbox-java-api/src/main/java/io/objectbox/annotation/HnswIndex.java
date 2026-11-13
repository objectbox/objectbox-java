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

package io.objectbox.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Parameters to configure HNSW-based approximate nearest neighbor (ANN) search. Some of the parameters can influence
 * index construction and searching. Changing parameters of the index construction causes re-indexing, which can take a
 * while due to the complex nature of HNSW (see each parameter).
 * <p>
 * To trade search speed for quality (recall), prefer the {@link #searchLevel()} (and if needed, the
 * {@link #buildLevel()}) over tuning the single parameters.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface HnswIndex {

    /**
     * Dimensions of vectors; vector data with fewer dimensions are ignored. Vectors with more dimensions than specified
     * here are only evaluated up to the given dimension value. Changing this value causes re-indexing.
     * <p>
     * Required, unless the index uses {@link HnswFlags#lateInit() late init}.
     */
    long dimensions() default 0;

    /**
     * Aka "M": the max number of connections per node (default: 30). Higher numbers increase the graph connectivity,
     * which can lead to more accurate search results. However, higher numbers also increase the indexing time and
     * resource usage. Try e.g. 16 for faster but less accurate results, or 64 for more accurate results. Changing this
     * value causes re-indexing.
     * <p>
     * See also {@link #buildLevel()} to set this together with the {@link #indexingSearchCount()}.
     */
    long neighborsPerNode() default 0;

    /**
     * Aka "efConstruction": the number of neighbor searched for while indexing (default: 128; an existing index keeps
     * the value it was built with, e.g. 100, the default before 6.0). The higher the value, the more accurate the
     * search, but the longer the indexing. If indexing time is not a major concern, a value of at least 200 is
     * recommended to improve search quality. Changing this value causes re-indexing.
     * <p>
     * See also {@link #buildLevel()} to set this together with the {@link #neighborsPerNode()}.
     */
    long indexingSearchCount() default 0;

    /**
     * See {@link HnswFlags}.
     */
    HnswFlags flags() default @HnswFlags;

    /**
     * The distance type used for the HNSW index. Changing this value causes re-indexing.
     */
    VectorDistanceType distanceType() default VectorDistanceType.DEFAULT;

    /**
     * When repairing the graph after a node was removed, this gives the probability of adding backlinks to the repaired
     * neighbors. The default is 1.0 (aka "always") as this should be worth a bit of extra costs as it improves the
     * graph's quality.
     */
    float reparationBacklinkProbability() default 1.0F;

    /**
     * A non-binding hint at the maximum size of the vector cache in KB (default: 2097152 or 2 GB/GiB). The actual size
     * max cache size may be altered according to device and/or runtime settings. The vector cache is used to store
     * vectors in memory to speed up search and indexing.
     * <p>
     * Note 1: cache chunks are allocated only on demand, when they are actually used. Thus, smaller datasets will use
     * less memory.
     * <p>
     * Note 2: the cache is for one specific HNSW index; e.g. each index has its own cache.
     * <p>
     * Note 3: the memory consumption can temporarily exceed the cache size, e.g. for large changes, it can double due
     * to multi-version transactions.
     */
    long vectorCacheHintSizeKB() default 0;

    /**
     * The quantization (compression) of the vectors held in memory for searching; see {@link VectorQuantization} for
     * the available types and the default.
     * <p>
     * This only affects the vectors in memory, not the stored ones; thus, changing this value does not cause
     * re-indexing.
     */
    VectorQuantization quantization() default VectorQuantization.DEFAULT;

    /**
     * With quantization: the oversample factor that corrects for the quantization error, for searches that do not set
     * one: they explore this factor times as many candidates (rounded, at least the requested result count).
     * <p>
     * 0 (the default) selects 1.25 with rescoring and 1.5 without it; other values must be at least 1. Higher values
     * increase recall at the cost of speed. Does not cause re-indexing.
     * <p>
     * For power users only; prefer the {@link #searchLevel()}. The search levels of
     * {@link VectorQuantization#TURBO_QUANT_1}, {@link VectorQuantization#TURBO_QUANT_1_5} and
     * {@link VectorQuantization#TURBO_QUANT_2} set their candidate counts explicitly and ignore this value.
     */
    float quantizationOversample() default 0;

    /**
     * With quantization and rescoring: sets how many of the explored candidates are rescored with the full-precision
     * vectors, for searches that do not set it: only this factor times the requested result count of the candidates
     * with the nearest estimates (rounded, at least the result count, at most the explored candidates), i.e. fewer
     * object reads when exploring many candidates.
     * <p>
     * 0 (the default) grows with the oversample factor of the search: max(1.5, sqrt(oversample)); other values must be
     * at least 1. Does not cause re-indexing.
     * <p>
     * For power users only; prefer the {@link #searchLevel()}. The search levels of
     * {@link VectorQuantization#TURBO_QUANT_1}, {@link VectorQuantization#TURBO_QUANT_1_5} and
     * {@link VectorQuantization#TURBO_QUANT_2} set their rescore counts explicitly and ignore this value.
     */
    float quantizationRescoreFactor() default 0;

    /**
     * The search level of queries that do not set one (default: {@link VectorSearchLevel#L4_BALANCED}); see
     * {@link VectorSearchLevel}.
     * <p>
     * This only affects searching; thus, changing this value does not cause re-indexing.
     */
    VectorSearchLevel searchLevel() default VectorSearchLevel.DEFAULT;

    /**
     * Sets the {@link #neighborsPerNode()} and the {@link #indexingSearchCount()} of a build level; see
     * {@link VectorBuildLevel}. If those are set too, they take precedence over the level.
     * <p>
     * Changing this value causes re-indexing (if the resulting values change).
     */
    VectorBuildLevel buildLevel() default VectorBuildLevel.DEFAULT;

    /**
     * For queries combining nearest neighbor search with other conditions: if the other conditions (using indexes and
     * relations) leave at most this many candidates, the nearest neighbors among the matching candidates are found by
     * brute force instead of by a graph search with a filter. Brute force results always carry exact distances.
     * <p>
     * Queries use this value unless they set their own. 0 (the default) selects 10,000. Does not cause re-indexing.
     */
    long bruteForceMaxCandidates() default 0;

    /**
     * For queries combining nearest neighbor search with other conditions: the graph search, which accepts only
     * objects matching the other conditions as results, explores at most this many nodes; if few objects match, it
     * would otherwise walk through much of the graph. At the limit, the query returns the matching objects found so
     * far, which may be fewer than the requested result count.
     * <p>
     * Queries use this value unless they set their own. 0 (the default) selects 25,000. Raise it for searches that
     * explore many nodes, e.g. with a high result count. Does not cause re-indexing.
     */
    long filteredSearchMaxNodes() default 0;

}
