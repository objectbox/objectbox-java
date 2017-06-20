package io.objectbox;

import com.google.flatbuffers.FlatBufferBuilder;

import java.util.ArrayList;
import java.util.List;

import io.objectbox.annotation.apihint.Internal;
import io.objectbox.model.IdUid;
import io.objectbox.model.Model;
import io.objectbox.model.ModelEntity;
import io.objectbox.model.ModelProperty;

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

    public class PropertyBuilder {
        boolean finished;

        PropertyBuilder(String name, String targetEntityName, String virtualTarget, int type) {
            int propertyNameOffset = fbb.createString(name);
            int targetEntityOffset = targetEntityName != null ? fbb.createString(targetEntityName) : 0;
            int virtualTargetOffset = virtualTarget != null ? fbb.createString(virtualTarget) : 0;
            ModelProperty.startModelProperty(fbb);
            ModelProperty.addName(fbb, propertyNameOffset);
            if (targetEntityOffset != 0) {
                ModelProperty.addTargetEntity(fbb, targetEntityOffset);
            }
            if (virtualTargetOffset != 0) {
                ModelProperty.addVirtualTarget(fbb, virtualTargetOffset);
            }
            ModelProperty.addType(fbb, type);
        }

        public PropertyBuilder id(int id, long uid) {
            checkNotFinished();
            int idOffset = IdUid.createIdUid(fbb, id, uid);
            ModelProperty.addId(fbb, idOffset);
            return this;
        }

        public PropertyBuilder indexId(int indexId, long indexUid) {
            checkNotFinished();
            int idOffset = IdUid.createIdUid(fbb, indexId, indexUid);
            ModelProperty.addIndexId(fbb, idOffset);
            return this;
        }

        public PropertyBuilder flags(int flags) {
            checkNotFinished();
            ModelProperty.addFlags(fbb, flags);
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
            return ModelProperty.endModelProperty(fbb);
        }
    }

    public class EntityBuilder {
        final String name;
        final List<Integer> propertyOffsets = new ArrayList<>();

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

        public PropertyBuilder property(String name, String targetEntityName, int type) {
            return property(name, targetEntityName, null, type);
        }

        public PropertyBuilder property(String name, String targetEntityName, String virtualTarget, int type) {
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

        public ModelBuilder entityDone() {
            checkNotFinished();
            checkFinishProperty();
            finished = true;
            int testEntityNameOffset = fbb.createString(name);
            int propertiesOffset = createVector(propertyOffsets);
            ModelEntity.startModelEntity(fbb);
            ModelEntity.addName(fbb, testEntityNameOffset);
            ModelEntity.addProperties(fbb, propertiesOffset);
            if (id != null || uid != null) {
                int idOffset = IdUid.createIdUid(fbb, id, uid);
                ModelEntity.addId(fbb, idOffset);
            }
            if (lastPropertyId != null) {
                int idOffset = IdUid.createIdUid(fbb, lastPropertyId, lastPropertyUid);
                ModelEntity.addLastPropertyId(fbb, idOffset);
            }
            if(flags != null) {
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
        int offset = Model.endModel(fbb);

        fbb.finish(offset);
        return fbb.sizedByteArray();
    }
}
