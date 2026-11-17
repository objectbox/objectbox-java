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
 * The quantization (compression) of the vectors an {@link HnswIndex} holds in memory for searching.
 * <p>
 * This only affects the vectors in memory: the vectors stored in the database always stay unchanged (full-precision
 * 32-bit floats). Thus, changing the quantization requires no data migration and no re-indexing.
 * <p>
 * Which type to use (bytes per vector of d dimensions in parentheses; full precision: 4 d):
 * <ul>
 *     <li>{@link #TURBO_QUANT_4} (d / 2 + 12): the best all-rounder; at the same recall, it is as fast as or faster
 *     than the other types on most data, at about 1/8 of the memory of full-precision vectors.</li>
 *     <li>{@link #SCALAR_QUANT_8} (d + 8): for low to medium dimensions (e.g. up to 256) if twice the memory of
 *     TURBO_QUANT_4 is acceptable; the most accurate quantized distances, thus the highest recall without rescoring
 *     (search levels 1-3) and the fastest type at a medium recall (e.g. 0.9) on such data; slower than TURBO_QUANT_4
 *     at high dimensions.</li>
 *     <li>{@link #TURBO_QUANT_2}, {@link #TURBO_QUANT_1_5} and {@link #TURBO_QUANT_1} (d / 4, 3 d / 16 and d / 8, each
 *     + 12): only to save memory at high dimensions (e.g. 768+, for TURBO_QUANT_1 1000+); in this order, less memory
 *     and less recall; lower recall than TURBO_QUANT_4 at its speed, slower than TURBO_QUANT_4 at its recall; avoid
 *     them on low dimensions (e.g. 100-128), where they lose much recall.</li>
 *     <li>{@link #TURBO_QUANT_3} (3 d / 8 + 12): rarely useful; between TURBO_QUANT_2 and TURBO_QUANT_4 in memory at
 *     high dimensions, but slower than TURBO_QUANT_4 at about its recall; at up to 128 dimensions, it takes more memory
 *     than TURBO_QUANT_4.</li>
 *     <li>{@link #NONE} (4 d): small data sets, vectors with less than 32 dimensions, or if the exact distances are
 *     needed at every {@link VectorSearchLevel search level}.</li>
 * </ul>
 * The codes are padded to multiples of 32 bytes (e.g. at 100-128 dimensions, TURBO_QUANT_1, TURBO_QUANT_1_5 and
 * TURBO_QUANT_2 take 44 bytes, TURBO_QUANT_4 76).
 */
public enum VectorQuantization {

    /**
     * Not set; the default applies: a new index uses {@link #TURBO_QUANT_4} if the vectors have at least 32 dimensions
     * and the distance type is not {@link VectorDistanceType#GEO}, otherwise {@link #NONE}.
     * <p>
     * An existing index keeps its quantization ({@link #NONE} if it was created before version 6.0), unless a change
     * of other parameters re-indexes it (e.g. a different build level); then it is treated as new.
     */
    DEFAULT,

    /**
     * No quantization; the full-precision vectors are used.
     */
    NONE,

    /**
     * TurboQuant with 1 bit per dimension ("TQ1"); needs high dimensions (e.g. 1000+).
     * <p>
     * Its search levels rescore from level 3 on (all explored candidates), to compensate for the low precision.
     */
    TURBO_QUANT_1,

    /**
     * TurboQuant with 1.5 bits per dimension ("TQ1.5"; 1-bit codes over the vector zero-extended to 1.5x its
     * dimensions); for high dimensions (e.g. 1000+), between {@link #TURBO_QUANT_1} and {@link #TURBO_QUANT_2} in
     * memory and recall.
     * <p>
     * Its search levels rescore from level 3 on, to compensate for the low precision.
     */
    TURBO_QUANT_1_5,

    /**
     * TurboQuant with 2 bits per dimension ("TQ2"); for high dimensions (e.g. 768+).
     * <p>
     * Its search levels rescore from level 3 on, to compensate for the low precision.
     */
    TURBO_QUANT_2,

    /**
     * TurboQuant with 3 bits per dimension ("TQ3"); rarely useful (see above).
     */
    TURBO_QUANT_3,

    /**
     * TurboQuant with 4 bits per dimension ("TQ4"): the default and the recommended quantization.
     * <p>
     * It uses about 1/8 of the memory of the full-precision vectors.
     */
    TURBO_QUANT_4,

    /**
     * Scalar quantization with 8 bits per dimension ("SQ8"; int8 with one scale per vector, training-free); for low to
     * medium dimensions (e.g. up to 256).
     * <p>
     * It uses about 1/4 of the memory of the full-precision vectors.
     */
    SCALAR_QUANT_8
}
