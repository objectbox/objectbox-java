/*
 * Copyright 2024 ObjectBox Ltd.
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
 * index construction and searching. Changing these values causes re-indexing, which can take a while due to the complex
 * nature of HNSW.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface HnswIndex {

    /**
     * Dimensions of vectors; vector data with fewer dimensions are ignored. Vectors with more dimensions than specified
     * here are only evaluated up to the given dimension value. Changing this value causes re-indexing.
     */
    long dimensions();

    /**
     * Aka "M": the max number of connections per node (default: 30). Higher numbers increase the graph connectivity,
     * which can lead to more accurate search results. However, higher numbers also increase the indexing time and
     * resource usage. Try e.g. 16 for faster but less accurate results, or 64 for more accurate results. Changing this
     * value causes re-indexing.
     */
    long neighborsPerNode() default 0;

    /**
     * Aka "efConstruction": the number of neighbor searched for while indexing (default: 100). The higher the value,
     * the more accurate the search, but the longer the indexing. If indexing time is not a major concern, a value of at
     * least 200 is recommended to improve search quality. Changing this value causes re-indexing.
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

}
