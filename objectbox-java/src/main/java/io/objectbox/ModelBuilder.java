/*
 * Copyright 2017-2025 ObjectBox Ltd.
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

import io.objectbox.annotation.ExternalName;
import io.objectbox.annotation.ExternalType;
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
 * Builds a flatbuffer representation of the database model to be passed to {@link BoxStoreBuilder}.
 * <p>
 * This is an internal API that should only be called by the generated MyObjectBox code.
 */
@Internal
public class ModelBuilder {

    /**
     * The version of the model (structure). The database verifies it supports this version of a model.
     * <p>
     * Note this is different from the "modelVersion" in the model JSON file, which only refers to the JSON schema.
     */
    private static final int MODEL_VERSION = 2;
    private static final String DEFAULT_NAME = "default";
    private static final int DEFAULT_VERSION = 1;

    private final FlatBufferBuilder fbb = new FlatBufferBuilder();
    private final List<Integer> entityOffsets = new ArrayList<>();

    private long version = DEFAULT_VERSION;

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

        private final int propertyNameOffset;
        private final int targetEntityOffset;
        private final int virtualTargetOffset;
        private final int type;

        private int secondaryNameOffset;
        private int id;
        private long uid;
        private int indexId;
        private long indexUid;
        private int indexMaxValueLength;
        private int externalNameOffset;
        private int externalType;
        private int hnswParamsOffset;
        private int flags;

        private PropertyBuilder(FlatBufferBuilder fbb, String name, @Nullable String targetEntityName,
                                @Nullable String virtualTarget, int type) {
            super(fbb);
            propertyNameOffset = fbb.createString(name);
            targetEntityOffset = targetEntityName != null ? fbb.createString(targetEntityName) : 0;
            virtualTargetOffset = virtualTarget != null ? fbb.createString(virtualTarget) : 0;
            this.type = type;
        }

        /**
         * Sets the Java name of a renamed property when using {@link io.objectbox.annotation.NameInDb}.
         */
        public PropertyBuilder secondaryName(String secondaryName) {
            checkNotFinished();
            secondaryNameOffset = getFbb().createString(secondaryName);
            return this;
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
         * Sets the {@link ExternalName} of this property.
         */
        public PropertyBuilder externalName(String externalName) {
            checkNotFinished();
            externalNameOffset = getFbb().createString(externalName);
            return this;
        }

        /**
         * Sets the {@link ExternalType} of this property. Should be one of {@link ExternalPropertyType}.
         */
        public PropertyBuilder externalType(int externalType) {
            checkNotFinished();
            this.externalType = externalType;
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

        /**
         * One or more of {@link io.objectbox.model.PropertyFlags}.
         */
        public PropertyBuilder flags(int flags) {
            checkNotFinished();
            this.flags = flags;
            return this;
        }

        @Override
        public int createFlatBufferTable(FlatBufferBuilder fbb) {
            ModelProperty.startModelProperty(fbb);
            ModelProperty.addName(fbb, propertyNameOffset);
            if (targetEntityOffset != 0) ModelProperty.addTargetEntity(fbb, targetEntityOffset);
            if (virtualTargetOffset != 0) ModelProperty.addVirtualTarget(fbb, virtualTargetOffset);
            ModelProperty.addType(fbb, type);
            if (secondaryNameOffset != 0) ModelProperty.addNameSecondary(fbb, secondaryNameOffset);
            if (id != 0) {
                int idOffset = IdUid.createIdUid(fbb, id, uid);
                ModelProperty.addId(fbb, idOffset);
            }
            if (indexId != 0) {
                int indexIdOffset = IdUid.createIdUid(fbb, indexId, indexUid);
                ModelProperty.addIndexId(fbb, indexIdOffset);
            }
            if (indexMaxValueLength > 0) ModelProperty.addMaxIndexValueLength(fbb, indexMaxValueLength);
            if (externalNameOffset != 0) ModelProperty.addExternalName(fbb, externalNameOffset);
            if (externalType != 0) ModelProperty.addExternalType(fbb, externalType);
            if (hnswParamsOffset != 0) ModelProperty.addHnswParams(fbb, hnswParamsOffset);
            if (flags != 0) ModelProperty.addFlags(fbb, flags);
            return ModelProperty.endModelProperty(fbb);
        }
    }

    public static class RelationBuilder extends PartBuilder {

        private final String name;
        private final int relationId;
        private final long relationUid;
        private final int targetEntityId;
        private final long targetEntityUid;

        private int externalNameOffset;
        private int externalType;

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
         * Sets the {@link ExternalName} of this relation.
         */
        public RelationBuilder externalName(String externalName) {
            checkNotFinished();
            externalNameOffset = getFbb().createString(externalName);
            return this;
        }

        /**
         * Sets the {@link ExternalType} of this relation. Should be one of {@link ExternalPropertyType}.
         */
        public RelationBuilder externalType(int externalType) {
            checkNotFinished();
            this.externalType = externalType;
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
            if (externalNameOffset != 0) ModelRelation.addExternalName(fbb, externalNameOffset);
            if (externalType != 0) ModelRelation.addExternalType(fbb, externalType);
            return ModelRelation.endModelRelation(fbb);
        }
    }

    public static class EntityBuilder extends PartBuilder {

        private final ModelBuilder model;
        private final String name;
        private final List<Integer> propertyOffsets = new ArrayList<>();
        private final List<Integer> relationOffsets = new ArrayList<>();

        private Integer id;
        private Long uid;
        private Integer lastPropertyId;
        private Long lastPropertyUid;
        @Nullable private String externalName;
        private Integer flags;
        @Nullable private PropertyBuilder propertyBuilder;
        @Nullable private RelationBuilder relationBuilder;

        private EntityBuilder(ModelBuilder model, FlatBufferBuilder fbb, String name) {
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

        /**
         * Sets the {@link ExternalName} of this entity.
         */
        public EntityBuilder externalName(String externalName) {
            checkNotFinished();
            this.externalName = externalName;
            return this;
        }

        /**
         * One or more of {@link io.objectbox.model.EntityFlags}.
         */
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

        /**
         * @param name The name of this property in the database.
         * @param targetEntityName For {@link io.objectbox.model.PropertyType#Relation}, the name of the target entity.
         * @param virtualTarget For {@link io.objectbox.model.PropertyType#Relation}, if this property does not really
         * exist in the source code and is a virtual one, the name of the field this is based on that actually exists.
         * Currently used for ToOne fields that create virtual target ID properties.
         * @param type The {@link io.objectbox.model.PropertyType}.
         */
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
            int nameOffset = fbb.createString(name);
            int externalNameOffset = externalName != null ? fbb.createString(externalName) : 0;
            int propertiesOffset = model.createVector(propertyOffsets);
            int relationsOffset = relationOffsets.isEmpty() ? 0 : model.createVector(relationOffsets);

            ModelEntity.startModelEntity(fbb);
            ModelEntity.addName(fbb, nameOffset);
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
            if (externalNameOffset != 0) ModelEntity.addExternalName(fbb, externalNameOffset);
            if (flags != null) ModelEntity.addFlags(fbb, flags);
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

    /**
     * Sets the user-defined version of the schema this represents. Defaults to 1.
     * <p>
     * Currently unused.
     */
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
        int nameOffset = fbb.createString(DEFAULT_NAME);
        int entityVectorOffset = createVector(entityOffsets);
        Model.startModel(fbb);
        Model.addName(fbb, nameOffset);
        Model.addModelVersion(fbb, MODEL_VERSION);
        Model.addVersion(fbb, version);
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
