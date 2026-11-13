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

package io.objectbox;

import javax.annotation.Nullable;

import io.objectbox.annotation.HnswIndex;
import io.objectbox.annotation.VectorBuildLevel;
import io.objectbox.annotation.VectorDistanceType;
import io.objectbox.annotation.VectorQuantization;
import io.objectbox.annotation.VectorSearchLevel;
import io.objectbox.flatbuffers.FlatBufferBuilder;
import io.objectbox.model.HnswDistanceType;
import io.objectbox.model.HnswParams;
import io.objectbox.model.HnswQuantization;

/**
 * The parameters of an HNSW index (vector search), like the ones of {@link HnswIndex}; see there for details.
 * <p>
 * Values that are not set (e.g. 0) use the defaults.
 */
public class HnswIndexParams {

    private final long dimensions;
    private long neighborsPerNode;
    private long indexingSearchCount;
    private int flags;
    private VectorDistanceType distanceType = VectorDistanceType.DEFAULT;
    private float reparationBacklinkProbability;
    private long vectorCacheHintSizeKb;
    private VectorQuantization quantization = VectorQuantization.DEFAULT;
    private float quantizationOversample;
    private float quantizationRescoreFactor;
    private VectorSearchLevel searchLevel = VectorSearchLevel.DEFAULT;
    private VectorBuildLevel buildLevel = VectorBuildLevel.DEFAULT;
    private long bruteForceMaxCandidates;
    private long filteredSearchMaxNodes;

    /**
     * See {@link HnswIndex#dimensions()}.
     */
    public HnswIndexParams(long dimensions) {
        this.dimensions = dimensions;
    }

    /**
     * See {@link HnswIndex#neighborsPerNode()}.
     */
    public HnswIndexParams neighborsPerNode(long neighborsPerNode) {
        this.neighborsPerNode = neighborsPerNode;
        return this;
    }

    /**
     * See {@link HnswIndex#indexingSearchCount()}.
     */
    public HnswIndexParams indexingSearchCount(long indexingSearchCount) {
        this.indexingSearchCount = indexingSearchCount;
        return this;
    }

    /**
     * See {@link HnswIndex#flags()}; one or more of {@link io.objectbox.model.HnswFlags}.
     */
    public HnswIndexParams flags(int flags) {
        this.flags = flags;
        return this;
    }

    /**
     * See {@link HnswIndex#distanceType()}.
     */
    public HnswIndexParams distanceType(VectorDistanceType distanceType) {
        checkNotNull(distanceType, "distanceType");
        this.distanceType = distanceType;
        return this;
    }

    /**
     * See {@link HnswIndex#reparationBacklinkProbability()}.
     */
    public HnswIndexParams reparationBacklinkProbability(float reparationBacklinkProbability) {
        this.reparationBacklinkProbability = reparationBacklinkProbability;
        return this;
    }

    /**
     * See {@link HnswIndex#vectorCacheHintSizeKB()}.
     */
    public HnswIndexParams vectorCacheHintSizeKb(long vectorCacheHintSizeKb) {
        this.vectorCacheHintSizeKb = vectorCacheHintSizeKb;
        return this;
    }

    /**
     * See {@link HnswIndex#quantization()}.
     */
    public HnswIndexParams quantization(VectorQuantization quantization) {
        checkNotNull(quantization, "quantization");
        this.quantization = quantization;
        return this;
    }

    /**
     * See {@link HnswIndex#quantizationOversample()}.
     */
    public HnswIndexParams quantizationOversample(float quantizationOversample) {
        this.quantizationOversample = quantizationOversample;
        return this;
    }

    /**
     * See {@link HnswIndex#quantizationRescoreFactor()}.
     */
    public HnswIndexParams quantizationRescoreFactor(float quantizationRescoreFactor) {
        this.quantizationRescoreFactor = quantizationRescoreFactor;
        return this;
    }

    /**
     * See {@link HnswIndex#searchLevel()}.
     */
    public HnswIndexParams searchLevel(VectorSearchLevel searchLevel) {
        checkNotNull(searchLevel, "searchLevel");
        this.searchLevel = searchLevel;
        return this;
    }

    /**
     * See {@link HnswIndex#buildLevel()}.
     */
    public HnswIndexParams buildLevel(VectorBuildLevel buildLevel) {
        checkNotNull(buildLevel, "buildLevel");
        this.buildLevel = buildLevel;
        return this;
    }

    /**
     * See {@link HnswIndex#bruteForceMaxCandidates()}.
     */
    public HnswIndexParams bruteForceMaxCandidates(long bruteForceMaxCandidates) {
        this.bruteForceMaxCandidates = bruteForceMaxCandidates;
        return this;
    }

    /**
     * See {@link HnswIndex#filteredSearchMaxNodes()}.
     */
    public HnswIndexParams filteredSearchMaxNodes(long filteredSearchMaxNodes) {
        this.filteredSearchMaxNodes = filteredSearchMaxNodes;
        return this;
    }

    /**
     * Adds these parameters as a {@link HnswParams} table to the given builder; values that are not set are not added.
     *
     * @return the offset of the table.
     */
    int addTo(FlatBufferBuilder fbb) {
        HnswParams.startHnswParams(fbb);
        // FlatBufferBuilder does not add values equal to the default (0), so not set values are absent
        HnswParams.addDimensions(fbb, dimensions);
        HnswParams.addNeighborsPerNode(fbb, neighborsPerNode);
        HnswParams.addIndexingSearchCount(fbb, indexingSearchCount);
        HnswParams.addFlags(fbb, flags);
        HnswParams.addDistanceType(fbb, toHnswDistanceType(distanceType));
        HnswParams.addReparationBacklinkProbability(fbb, reparationBacklinkProbability);
        HnswParams.addVectorCacheHintSizeKb(fbb, vectorCacheHintSizeKb);
        HnswParams.addQuantization(fbb, toHnswQuantization(quantization));
        HnswParams.addQuantizationOversample(fbb, quantizationOversample);
        HnswParams.addQuantizationRescoreFactor(fbb, quantizationRescoreFactor);
        HnswParams.addSearchLevel(fbb, searchLevel.ordinal()); // The ordinal is the level (DEFAULT: 0)
        HnswParams.addBuildLevel(fbb, buildLevel.ordinal()); // The ordinal is the level (DEFAULT: 0)
        HnswParams.addBruteForceMaxCandidates(fbb, bruteForceMaxCandidates);
        HnswParams.addFilteredSearchMaxNodes(fbb, filteredSearchMaxNodes);
        return HnswParams.endHnswParams(fbb);
    }

    private static short toHnswDistanceType(VectorDistanceType distanceType) {
        switch (distanceType) {
            case DEFAULT:
                return 0;
            case EUCLIDEAN:
                return HnswDistanceType.Euclidean;
            case COSINE:
                return HnswDistanceType.Cosine;
            case DOT_PRODUCT:
                return HnswDistanceType.DotProduct;
            case GEO:
                return HnswDistanceType.Geo;
            case DOT_PRODUCT_NON_NORMALIZED:
                return HnswDistanceType.DotProductNonNormalized;
            default:
                throw new IllegalArgumentException("Unknown distance type: " + distanceType);
        }
    }

    private static short toHnswQuantization(VectorQuantization quantization) {
        switch (quantization) {
            case DEFAULT:
                return HnswQuantization.Undefined;
            case NONE:
                return HnswQuantization.None;
            case TURBO_QUANT_1:
                return HnswQuantization.TurboQuant1;
            case TURBO_QUANT_1_5:
                return HnswQuantization.TurboQuant1_5;
            case TURBO_QUANT_2:
                return HnswQuantization.TurboQuant2;
            case TURBO_QUANT_3:
                return HnswQuantization.TurboQuant3;
            case TURBO_QUANT_4:
                return HnswQuantization.TurboQuant4;
            case SCALAR_QUANT_8:
                return HnswQuantization.ScalarQuant8;
            default:
                throw new IllegalArgumentException("Unknown quantization: " + quantization);
        }
    }

    private static void checkNotNull(@Nullable Object object, String name) {
        if (object == null) {
            throw new IllegalArgumentException(name + " must not be null.");
        }
    }
}
