/*
 * Copyright 2017-2025 ObjectBox Ltd. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import io.objectbox.annotation.HnswIndex;
import io.objectbox.annotation.apihint.Internal;
import io.objectbox.flatbuffers.FlatBufferBuilder;
import io.objectbox.model.ExternalPropertyType;
import io.objectbox.model.HnswDistanceType;
import io.objectbox.model.HnswFlags;
import io.objectbox.model.HnswParams;
import io.objectbox.model.IdUid;
import io.objectbox.model.Model;
import io.objectbox.model.ModelEntity;
import io.objectbox.model.ModelProperty;
import io.objectbox.model.ModelRelation;

// To learn how to use the FlatBuffers API see https://flatbuffers.dev/tutorial/
// Note: IdUid is a struct, not a table, and thus must be inlined

/**
 * Builds a flatbuffer representation of the database model to be passed when opening a store.
 * <p>
 * This is an internal API that should only be called by the generated MyObjectBox code.
 */
@Internal
public class ModelBuilder {
    private static final int MODEL_VERSION = 2;

    private final FlatBufferBuilder fbb = new FlatBufferBuilder();
    private final List<Integer> entityOffsets = new ArrayList<>();

    private long version = 1;

    private Integer lastEntityId;
    private Long lastEntityUid;

    private Integer lastIndexId;
    private Long lastIndexUid;

    private Integer lastRelationId;
    private Long lastRelationUid;

    /**
     * Base class for builders.
     * <p>
     * Methods adding properties to be used by {@link #createFlatBufferTable(FlatBufferBuilder)} should call
     * {@link #checkNotFinished()}.
     * <p>
     * The last call should be {@link #finish()}.
     */
    abstract static class PartBuilder {

        private final FlatBufferBuilder fbb;
        private boolean finished;

        PartBuilder(FlatBufferBuilder fbb) {
            this.fbb = fbb;
        }

        FlatBufferBuilder getFbb() {
            return fbb;
        }

        void checkNotFinished() {
            if (finished) {
                throw new IllegalStateException("Already finished");
            }
        }

        /**
         * Marks this as finished and returns {@link #createFlatBufferTable(FlatBufferBuilder)}.
         */
        public final int finish() {
            checkNotFinished();
            finished = true;
            return createFlatBufferTable(getFbb());
        }

        /**
         * Creates a flatbuffer table using the given builder and returns its offset.
         */
        public abstract int createFlatBufferTable(FlatBufferBuilder fbb);
    }

    public static class PropertyBuilder extends PartBuilder {

        private final int type;
        private final int virtualTargetOffset;
        private final int propertyNameOffset;
        private final int targetEntityOffset;

        private int secondaryNameOffset;
        private int flags;
        private int id;
        private long uid;
        private int indexId;
        private long indexUid;
        private int indexMaxValueLength;
        private int externalPropertyType;
        private int hnswParamsOffset;

        private PropertyBuilder(FlatBufferBuilder fbb, String name, @Nullable String targetEntityName,
                                @Nullable String virtualTarget, int type) {
            super(fbb);
            this.type = type;
            propertyNameOffset = fbb.createString(name);
            targetEntityOffset = targetEntityName != null ? fbb.createString(targetEntityName) : 0;
            virtualTargetOffset = virtualTarget != null ? fbb.createString(virtualTarget) : 0;
        }

        public PropertyBuilder id(int id, long uid) {
            checkNotFinished();
            this.id = id;
            this.uid = uid;
            return this;
        }

        public PropertyBuilder indexId(int indexId, long indexUid) {
            checkNotFinished();
            this.indexId = indexId;
            this.indexUid = indexUid;
            return this;
        }

        public PropertyBuilder indexMaxValueLength(int indexMaxValueLength) {
            checkNotFinished();
            this.indexMaxValueLength = indexMaxValueLength;
            return this;
        }

        /**
         * Sets the {@link ExternalPropertyType} constant for this.
         *
         * @return this builder.
         */
        public PropertyBuilder externalType(int externalPropertyType) {
            checkNotFinished();
            this.externalPropertyType = externalPropertyType;
            return this;
        }

        /**
         * Set parameters for {@link HnswIndex}.
         *
         * @param dimensions see {@link HnswIndex#dimensions()}.
         * @param neighborsPerNode see {@link HnswIndex#neighborsPerNode()}.
         * @param indexingSearchCount see {@link HnswIndex#indexingSearchCount()}.
         * @param flags see {@link HnswIndex#flags()}, mapped to {@link HnswFlags}.
         * @param distanceType see {@link HnswIndex#distanceType()}, mapped to {@link HnswDistanceType}.
         * @param reparationBacklinkProbability see {@link HnswIndex#reparationBacklinkProbability()}.
         * @param vectorCacheHintSizeKb see {@link HnswIndex#vectorCacheHintSizeKB()}.
         * @return this builder.
         */
        public PropertyBuilder hnswParams(long dimensions,
                                          @Nullable Long neighborsPerNode,
                                          @Nullable Long indexingSearchCount,
                                          @Nullable Integer flags,
                                          @Nullable Short distanceType,
                                          @Nullable Float reparationBacklinkProbability,
                                          @Nullable Long vectorCacheHintSizeKb) {
            checkNotFinished();
            FlatBufferBuilder fbb = getFbb();
            HnswParams.startHnswParams(fbb);
            HnswParams.addDimensions(fbb, dimensions);
            if (neighborsPerNode != null) {
                HnswParams.addNeighborsPerNode(fbb, neighborsPerNode);
            }
            if (indexingSearchCount != null) {
                HnswParams.addIndexingSearchCount(fbb, indexingSearchCount);
            }
            if (flags != null) {
                HnswParams.addFlags(fbb, flags);
            }
            if (distanceType != null) {
                HnswParams.addDistanceType(fbb, distanceType);
            }
            if (reparationBacklinkProbability != null) {
                HnswParams.addReparationBacklinkProbability(fbb, reparationBacklinkProbability);
            }
            if (vectorCacheHintSizeKb != null) {
                HnswParams.addVectorCacheHintSizeKb(fbb, vectorCacheHintSizeKb);
            }
            hnswParamsOffset = HnswParams.endHnswParams(fbb);
            return this;
        }

        public PropertyBuilder flags(int flags) {
            checkNotFinished();
            this.flags = flags;
            return this;
        }

        public PropertyBuilder secondaryName(String secondaryName) {
            checkNotFinished();
            secondaryNameOffset = getFbb().createString(secondaryName);
            return this;
        }

        @Override
        public int createFlatBufferTable(FlatBufferBuilder fbb) {
            ModelProperty.startModelProperty(fbb);
            ModelProperty.addName(fbb, propertyNameOffset);
            if (targetEntityOffset != 0) {
                ModelProperty.addTargetEntity(fbb, targetEntityOffset);
            }
            if (virtualTargetOffset != 0) {
                ModelProperty.addVirtualTarget(fbb, virtualTargetOffset);
            }
            if (secondaryNameOffset != 0) {
                ModelProperty.addNameSecondary(fbb, secondaryNameOffset);
            }
            if (id != 0) {
                int idOffset = IdUid.createIdUid(fbb, id, uid);
                ModelProperty.addId(fbb, idOffset);
            }
            if (indexId != 0) {
                int indexIdOffset = IdUid.createIdUid(fbb, indexId, indexUid);
                ModelProperty.addIndexId(fbb, indexIdOffset);
            }
            if (indexMaxValueLength > 0) {
                ModelProperty.addMaxIndexValueLength(fbb, indexMaxValueLength);
            }
            if (externalPropertyType != 0) {
                ModelProperty.addExternalType(fbb, externalPropertyType);
            }
            if (hnswParamsOffset != 0) {
                ModelProperty.addHnswParams(fbb, hnswParamsOffset);
            }
            ModelProperty.addType(fbb, type);
            if (flags != 0) {
                ModelProperty.addFlags(fbb, flags);
            }
            return ModelProperty.endModelProperty(fbb);
        }
    }

    public static class RelationBuilder extends PartBuilder {

        private final String name;
        private final int relationId;
        private final long relationUid;
        private final int targetEntityId;
        private final long targetEntityUid;
        private int externalPropertyType;

        private RelationBuilder(FlatBufferBuilder fbb, String name, int relationId, long relationUid,
                                int targetEntityId, long targetEntityUid) {
            super(fbb);
            this.name = name;
            this.relationId = relationId;
            this.relationUid = relationUid;
            this.targetEntityId = targetEntityId;
            this.targetEntityUid = targetEntityUid;
        }

        /**
         * Sets the {@link ExternalPropertyType} constant for this.
         *
         * @return this builder.
         */
        public RelationBuilder externalType(int externalPropertyType) {
            checkNotFinished();
            this.externalPropertyType = externalPropertyType;
            return this;
        }

        @Override
        public int createFlatBufferTable(FlatBufferBuilder fbb) {
            int nameOffset = fbb.createString(name);

            ModelRelation.startModelRelation(fbb);
            ModelRelation.addName(fbb, nameOffset);
            int relationIdOffset = IdUid.createIdUid(fbb, relationId, relationUid);
            ModelRelation.addId(fbb, relationIdOffset);
            int targetEntityIdOffset = IdUid.createIdUid(fbb, targetEntityId, targetEntityUid);
            ModelRelation.addTargetEntityId(fbb, targetEntityIdOffset);
            if (externalPropertyType != 0) {
                ModelRelation.addExternalType(fbb, externalPropertyType);
            }
            return ModelRelation.endModelRelation(fbb);
        }
    }

    public static class EntityBuilder extends PartBuilder {

        private final ModelBuilder model;
        final String name;
        final List<Integer> propertyOffsets = new ArrayList<>();
        final List<Integer> relationOffsets = new ArrayList<>();

        Integer id;
        Long uid;
        Integer flags;
        Integer lastPropertyId;
        Long lastPropertyUid;
        @Nullable PropertyBuilder propertyBuilder;
        @Nullable RelationBuilder relationBuilder;
        boolean finished;

        EntityBuilder(ModelBuilder model, FlatBufferBuilder fbb, String name) {
            super(fbb);
            this.model = model;
            this.name = name;
        }

        public EntityBuilder id(int id, long uid) {
            checkNotFinished();
            this.id = id;
            this.uid = uid;
            return this;
        }

        public EntityBuilder lastPropertyId(int lastPropertyId, long lastPropertyUid) {
            checkNotFinished();
            this.lastPropertyId = lastPropertyId;
            this.lastPropertyUid = lastPropertyUid;
            return this;
        }

        public EntityBuilder flags(int flags) {
            this.flags = flags;
            return this;
        }

        public PropertyBuilder property(String name, int type) {
            return property(name, null, type);
        }

        public PropertyBuilder property(String name, @Nullable String targetEntityName, int type) {
            return property(name, targetEntityName, null, type);
        }

        public PropertyBuilder property(String name, @Nullable String targetEntityName, @Nullable String virtualTarget,
                                        int type) {
            checkNotFinished();
            finishPropertyOrRelation();
            propertyBuilder = new PropertyBuilder(getFbb(), name, targetEntityName, virtualTarget, type);
            return propertyBuilder;
        }

        public RelationBuilder relation(String name, int relationId, long relationUid, int targetEntityId,
                                        long targetEntityUid) {
            checkNotFinished();
            finishPropertyOrRelation();

            RelationBuilder relationBuilder = new RelationBuilder(getFbb(), name, relationId, relationUid, targetEntityId, targetEntityUid);
            this.relationBuilder = relationBuilder;
            return relationBuilder;
        }

        private void finishPropertyOrRelation() {
            if (propertyBuilder != null && relationBuilder != null) {
                throw new IllegalStateException("Must not build property and relation at the same time.");
            }
            if (propertyBuilder != null) {
                propertyOffsets.add(propertyBuilder.finish());
                propertyBuilder = null;
            }
            if (relationBuilder != null) {
                relationOffsets.add(relationBuilder.finish());
                relationBuilder = null;
            }
        }

        public ModelBuilder entityDone() {
            // Make sure any pending property or relation is finished first
            checkNotFinished();
            finishPropertyOrRelation();
            model.entityOffsets.add(finish());
            return model;
        }

        @Override
        public int createFlatBufferTable(FlatBufferBuilder fbb) {
            int testEntityNameOffset = fbb.createString(name);
            int propertiesOffset = model.createVector(propertyOffsets);
            int relationsOffset = relationOffsets.isEmpty() ? 0 : model.createVector(relationOffsets);

            ModelEntity.startModelEntity(fbb);
            ModelEntity.addName(fbb, testEntityNameOffset);
            ModelEntity.addProperties(fbb, propertiesOffset);
            if (relationsOffset != 0) ModelEntity.addRelations(fbb, relationsOffset);
            if (id != null && uid != null) {
                int idOffset = IdUid.createIdUid(fbb, id, uid);
                ModelEntity.addId(fbb, idOffset);
            }
            if (lastPropertyId != null) {
                int idOffset = IdUid.createIdUid(fbb, lastPropertyId, lastPropertyUid);
                ModelEntity.addLastPropertyId(fbb, idOffset);
            }
            if (flags != null) {
                ModelEntity.addFlags(fbb, flags);
            }
            return ModelEntity.endModelEntity(fbb);
        }

    }

    private int createVector(List<Integer> offsets) {
        int[] offsetArray = new int[offsets.size()];
        for (int i = 0; i < offsets.size(); i++) {
            offsetArray[i] = offsets.get(i);
        }
        return fbb.createVectorOfTables(offsetArray);
    }

    public ModelBuilder version(long version) {
        this.version = version;
        return this;
    }

    public EntityBuilder entity(String name) {
        return new EntityBuilder(this, fbb, name);
    }

    public ModelBuilder lastEntityId(int lastEntityId, long lastEntityUid) {
        this.lastEntityId = lastEntityId;
        this.lastEntityUid = lastEntityUid;
        return this;
    }

    public ModelBuilder lastIndexId(int lastIndexId, long lastIndexUid) {
        this.lastIndexId = lastIndexId;
        this.lastIndexUid = lastIndexUid;
        return this;
    }

    public ModelBuilder lastRelationId(int lastRelationId, long lastRelationUid) {
        this.lastRelationId = lastRelationId;
        this.lastRelationUid = lastRelationUid;
        return this;
    }

    public byte[] build() {
        int nameOffset = fbb.createString("default");
        int entityVectorOffset = createVector(entityOffsets);
        Model.startModel(fbb);
        Model.addName(fbb, nameOffset);
        Model.addModelVersion(fbb, MODEL_VERSION);
        Model.addVersion(fbb, 1);
        Model.addEntities(fbb, entityVectorOffset);
        if (lastEntityId != null) {
            int idOffset = IdUid.createIdUid(fbb, lastEntityId, lastEntityUid);
            Model.addLastEntityId(fbb, idOffset);
        }
        if (lastIndexId != null) {
            int idOffset = IdUid.createIdUid(fbb, lastIndexId, lastIndexUid);
            Model.addLastIndexId(fbb, idOffset);
        }
        if (lastRelationId != null) {
            int idOffset = IdUid.createIdUid(fbb, lastRelationId, lastRelationUid);
            Model.addLastRelationId(fbb, idOffset);
        }
        int offset = Model.endModel(fbb);

        fbb.finish(offset);
        return fbb.sizedByteArray();
    }
}
