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

/**
 * Build levels of an {@link HnswIndex}: they define the essential parameters for building the HNSW graph, the
 * {@link HnswIndex#neighborsPerNode() neighbors per node} ("M") and the
 * {@link HnswIndex#indexingSearchCount() indexing search count} ("efConstruction") together.
 * <p>
 * Start with the default {@link #L4_BALANCED} and adjust from there if you want to optimize the graph further. The
 * trade-off is the graph's build time and memory against search quality (recall) and speed.
 * <p>
 * Unlike the {@link VectorSearchLevel search level}, this applies when vectors are inserted (indexed); thus, a changed
 * build level causes re-indexing (rebuilding the entire graph, which can take a while).
 * <p>
 * Higher levels pay off on "hard" data only (e.g. GloVe word embeddings), where the default graph limits the recall
 * even at the highest search levels; on "easy" data, which reaches a high recall already with the default, they cost
 * build time, memory and search speed.
 * <p>
 * The values' comments give the underlying neighbors per node (M) and indexing search count (efConstruction), which
 * you can use as a starting point if you want to optimize them further. These may change in future versions; existing
 * indexes keep the values they were built with.
 */
public enum VectorBuildLevel {

    // Note: the ordinal is the level number (DEFAULT: 0); do not reorder.

    /**
     * Not set: the index uses its {@link HnswIndex#neighborsPerNode()} and {@link HnswIndex#indexingSearchCount()},
     * which default to the values of {@link #L4_BALANCED} for a new index.
     */
    DEFAULT,

    /**
     * M 10 / efConstruction 64: about 0.3-0.6x the build time of L4.
     */
    L1_FASTEST,

    /**
     * M 20 / efConstruction 64.
     */
    L2_VERY_FAST,

    /**
     * M 20 / efConstruction 128.
     */
    L3_FAST,

    /**
     * M 30 / efConstruction 128: the default and a good starting point.
     */
    L4_BALANCED,

    /**
     * M 32 / efConstruction 160: recommended if you can afford a longer build time.
     */
    L5_BALANCED_QUALITY,

    /**
     * M 32 / efConstruction 256: about 1.7-2.8x the build time of L4.
     */
    L6_QUALITY,

    /**
     * M 48 / efConstruction 192.
     */
    L7_QUALITY_HIGH,

    /**
     * M 64 / efConstruction 192.
     */
    L8_QUALITY_HIGHER,

    /**
     * M 96 / efConstruction 128: about 1.6-3.4x the build time of L4; reaches a recall of 0.99 on hard data.
     */
    L9_QUALITY_ULTRA,

    /**
     * M 96 / efConstruction 256.
     */
    L10_QUALITY_MAX
}
