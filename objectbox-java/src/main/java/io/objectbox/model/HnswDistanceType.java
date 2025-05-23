/*
 * Copyright 2025 ObjectBox Ltd. All rights reserved.
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

// automatically generated by the FlatBuffers compiler, do not modify

package io.objectbox.model;

/**
 * The distance algorithm used by an HNSW index (vector search).
 */
@SuppressWarnings("unused")
public final class HnswDistanceType {
  private HnswDistanceType() { }
  /**
   * Not a real type, just best practice (e.g. forward compatibility)
   */
  public static final short Unknown = 0;
  /**
   * The default; typically "Euclidean squared" internally.
   */
  public static final short Euclidean = 1;
  /**
   * Cosine similarity compares two vectors irrespective of their magnitude (compares the angle of two vectors).
   * Often used for document or semantic similarity.
   * Value range: 0.0 - 2.0 (0.0: same direction, 1.0: orthogonal, 2.0: opposite direction)
   */
  public static final short Cosine = 2;
  /**
   * For normalized vectors (vector length == 1.0), the dot product is equivalent to the cosine similarity.
   * Because of this, the dot product is often preferred as it performs better.
   * Value range (normalized vectors): 0.0 - 2.0 (0.0: same direction, 1.0: orthogonal, 2.0: opposite direction)
   */
  public static final short DotProduct = 3;
  /**
   * For geospatial coordinates aka latitude/longitude pairs.
   * Note, that the vector dimension must be 2, with the latitude being the first element and longitude the second.
   * Internally, this uses haversine distance.
   */
  public static final short Geo = 6;
  /**
   * A custom dot product similarity measure that does not require the vectors to be normalized.
   * Note: this is no replacement for cosine similarity (like DotProduct for normalized vectors is).
   * The non-linear conversion provides a high precision over the entire float range (for the raw dot product).
   * The higher the dot product, the lower the distance is (the nearer the vectors are).
   * The more negative the dot product, the higher the distance is (the farther the vectors are).
   * Value range: 0.0 - 2.0 (nonlinear; 0.0: nearest, 1.0: orthogonal, 2.0: farthest)
   */
  public static final short DotProductNonNormalized = 10;

  public static final String[] names = { "Unknown", "Euclidean", "Cosine", "DotProduct", "", "", "Geo", "", "", "", "DotProductNonNormalized", };

  public static String name(int e) { return names[e]; }
}

