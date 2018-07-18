/*
 * Copyright 2017 ObjectBox Ltd. All rights reserved.
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

import com.google.flatbuffers.FlatBufferBuilder;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import io.objectbox.annotation.apihint.Internal;
import io.objectbox.model.IdUid;
import io.objectbox.model.Model;
import io.objectbox.model.ModelEntity;
import io.objectbox.model.ModelProperty;
import io.objectbox.model.ModelRelation;

// Remember: IdUid is a struct, not a table, and thus must be inlined
@SuppressWarnings("WeakerAccess,UnusedReturnValue, unused")
@Internal
public class ModelBuilder {
    private static final int MODEL_VERSION = 2;

    final FlatBufferBuilder fbb = new FlatBufferBuilder();
    final List<Integer> entityOffsets = new ArrayList<>();

    long version = 1;

    Integer lastEntityId;
    Long lastEntityUid;

    Integer lastIndexId;
    Long lastIndexUid;

    Integer lastRelationId;
    Long lastRelationUid;

    public class PropertyBuilder {
        private final int type;
        private final int virtualTargetOffset;
        private final int propertyNameOffset;
        private final int targetEntityOffset;

        private int secondaryNameOffset;
        boolean finished;
        private int flags;
        private int id;
        private long uid;
        private int indexId;
        private long indexUid;
        private int indexMaxValueLength;

        PropertyBuilder(String name, @Nullable String targetEntityName, @Nullable String virtualTarget, int type) {
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

        public PropertyBuilder flags(int flags) {
            checkNotFinished();
            this.flags = flags;
            return this;
        }

        public PropertyBuilder secondaryName(String secondaryName) {
            checkNotFinished();
            secondaryNameOffset = fbb.createString(secondaryName);
            return this;
        }

        private void checkNotFinished() {
            if (finished) {
                throw new IllegalStateException("Already finished");
            }
        }

        public int finish() {
            checkNotFinished();
            finished = true;
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
            ModelProperty.addType(fbb, type);
            if (flags != 0) {
                ModelProperty.addFlags(fbb, flags);
            }
            return ModelProperty.endModelProperty(fbb);
        }
    }

    public class EntityBuilder {
        final String name;
        final List<Integer> propertyOffsets = new ArrayList<>();
        final List<Integer> relationOffsets = new ArrayList<>();

        Integer id;
        Long uid;
        Integer flags;
        Integer lastPropertyId;
        Long lastPropertyUid;
        PropertyBuilder propertyBuilder;
        boolean finished;

        EntityBuilder(String name) {
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

        private void checkNotFinished() {
            if (finished) {
                throw new IllegalStateException("Already finished");
            }
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
            checkFinishProperty();
            propertyBuilder = new PropertyBuilder(name, targetEntityName, virtualTarget, type);
            return propertyBuilder;
        }

        void checkFinishProperty() {
            if (propertyBuilder != null) {
                propertyOffsets.add(propertyBuilder.finish());
                propertyBuilder = null;
            }
        }

        public EntityBuilder relation(String name, int relationId, long relationUid, int targetEntityId,
                                      long targetEntityUid) {
            checkNotFinished();
            checkFinishProperty();

            int propertyNameOffset = fbb.createString(name);

            ModelRelation.startModelRelation(fbb);
            ModelRelation.addName(fbb, propertyNameOffset);
            int relationIdOffset = IdUid.createIdUid(fbb, relationId, relationUid);
            ModelRelation.addId(fbb, relationIdOffset);
            int targetEntityIdOffset = IdUid.createIdUid(fbb, targetEntityId, targetEntityUid);
            ModelRelation.addTargetEntityId(fbb, targetEntityIdOffset);
            relationOffsets.add(ModelRelation.endModelRelation(fbb));

            return this;
        }

        public ModelBuilder entityDone() {
            checkNotFinished();
            checkFinishProperty();
            finished = true;
            int testEntityNameOffset = fbb.createString(name);
            int propertiesOffset = createVector(propertyOffsets);
            int relationsOffset = relationOffsets.isEmpty() ? 0 : createVector(relationOffsets);

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
            entityOffsets.add(ModelEntity.endModelEntity(fbb));
            return ModelBuilder.this;
        }
    }

    int createVector(List<Integer> offsets) {
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
        return new EntityBuilder(name);
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
