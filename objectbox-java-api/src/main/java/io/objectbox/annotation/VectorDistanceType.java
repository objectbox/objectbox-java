/*
 * Copyright 2024-2025 ObjectBox Ltd.
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
 * The vector distance algorithm used by an {@link HnswIndex} (vector search).
 */
public enum VectorDistanceType {

    /**
     * The default; currently {@link #EUCLIDEAN}.
     */
    DEFAULT,

    /**
     * Typically "Euclidean squared" internally.
     */
    EUCLIDEAN,

    /**
     * Cosine similarity compares two vectors irrespective of their magnitude (compares the angle of two vectors).
     * <p>
     * Often used for document or semantic similarity.
     * <p>
     * Value range: 0.0 - 2.0 (0.0: same direction, 1.0: orthogonal, 2.0: opposite direction)
     */
    COSINE,

    /**
     * For normalized vectors (vector length == 1.0), the dot product is equivalent to the cosine similarity.
     * <p>
     * Because of this, the dot product is often preferred as it performs better.
     * <p>
     * Value range (normalized vectors): 0.0 - 2.0 (0.0: same direction, 1.0: orthogonal, 2.0: opposite direction)
     */
    DOT_PRODUCT,

    /**
     * For geospatial coordinates, more specifically latitude and longitude pairs.
     * <p>
     * Note, the vector dimension should be 2, with the latitude being the first element and longitude the second.
     * If the vector has more than 2 dimensions, only the first 2 dimensions are used.
     * If the vector has fewer than 2 dimensions, the distance is always zero.
     * <p>
     * Internally, this uses haversine distance.
     * <p>
     * Value range: 0 km - 6371 * π km (approx. 20015.09 km; half the Earth's circumference)
     */
    GEO,

    /**
     * A custom dot product similarity measure that does not require the vectors to be normalized.
     * <p>
     * Note: this is no replacement for cosine similarity (like DotProduct for normalized vectors is). The non-linear
     * conversion provides a high precision over the entire float range (for the raw dot product). The higher the dot
     * product, the lower the distance is (the nearer the vectors are). The more negative the dot product, the higher
     * the distance is (the farther the vectors are).
     * <p>
     * Value range: 0.0 - 2.0 (nonlinear; 0.0: nearest, 1.0: orthogonal, 2.0: farthest)
     */
    DOT_PRODUCT_NON_NORMALIZED
}
