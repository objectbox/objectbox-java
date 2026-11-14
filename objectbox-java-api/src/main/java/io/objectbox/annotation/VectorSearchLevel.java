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
 * Search levels of an {@link HnswIndex}: the preferred way to trade search speed for quality (recall).
 * <p>
 * Start from {@link #L4_BALANCED} (the default) and adjust from there if you want to optimize the speed/quality
 * trade-off. The levels range from the fastest search (lowest recall) to the highest recall (slowest search); each
 * level costs roughly 1.3-2x the time of the previous one.
 * <p>
 * With {@link VectorQuantization quantization}, levels 1-3 do not rescore, so their result distances are estimates;
 * levels 4 and higher rescore with the full-precision vectors, so their result distances are exact
 * ({@link VectorQuantization#TURBO_QUANT_1}, {@link VectorQuantization#TURBO_QUANT_1_5} and
 * {@link VectorQuantization#TURBO_QUANT_2} already rescore at level 3).
 * <p>
 * Queries without a level search at the level of the index ({@link HnswIndex#searchLevel()}).
 * <p>
 * If the highest levels do not reach a recall target, build the graph at a higher {@link VectorBuildLevel} instead.
 */
public enum VectorSearchLevel {

    // Note: the ordinal is the level number (DEFAULT: 0); do not reorder.

    /**
     * Not set; the default applies: for an index, {@link #L4_BALANCED}. Not valid for a query.
     */
    DEFAULT,

    /**
     * The fastest: explores only as many candidates as results are requested; with quantization, no rescoring
     * (estimated distances).
     */
    L1_FASTEST,

    /**
     * With quantization, no rescoring (estimated distances).
     */
    L2_VERY_FAST,

    /**
     * With quantization, no rescoring (estimated distances) except for {@link VectorQuantization#TURBO_QUANT_1},
     * {@link VectorQuantization#TURBO_QUANT_1_5} and {@link VectorQuantization#TURBO_QUANT_2}.
     */
    L3_FAST,

    /**
     * The default and a good starting point; with quantization, the best candidates are rescored (exact distances).
     */
    L4_BALANCED,

    /**
     * If level 4 misses too many neighbors; reaches a recall of about 0.97 on "easy" data such as OpenAI embeddings.
     */
    L5_BALANCED_QUALITY,

    /**
     * If missing a neighbor matters (e.g. for RAG); reaches a recall of about 0.99 on "easy" data.
     */
    L6_QUALITY,

    /**
     * "Easy" data saturates here; higher levels pay off only on "hard" data (e.g. GloVe word embeddings).
     */
    L7_QUALITY_HIGH,

    /**
     * For "hard" data; on "easy" data, it costs time without gaining recall.
     */
    L8_QUALITY_HIGHER,

    /**
     * For "hard" data and high recall targets; a higher {@link VectorBuildLevel} is often faster.
     */
    L9_QUALITY_ULTRA,

    /**
     * The highest recall (slowest search); if it misses your recall target, use a higher {@link VectorBuildLevel}
     * instead.
     */
    L10_QUALITY_MAX
}
