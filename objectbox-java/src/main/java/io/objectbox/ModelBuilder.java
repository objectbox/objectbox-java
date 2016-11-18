package io.objectbox;

import com.google.flatbuffers.FlatBufferBuilder;

import java.util.ArrayList;
import java.util.List;

import io.objectbox.annotation.apihint.Internal;
import io.objectbox.model.Model;
import io.objectbox.model.ModelEntity;
import io.objectbox.model.ModelProperty;

@Internal
public class ModelBuilder {
    final FlatBufferBuilder fbb = new FlatBufferBuilder();
    final List<Integer> entityOffsets = new ArrayList<>();

    long version = 1;
    Long lastEntityId;
    Long lastIndexId;

    public class PropertyBuilder {
        boolean finished;

        public PropertyBuilder(String name, int type) {
            int propertyNameOffset = fbb.createString(name);
            ModelProperty.startModelProperty(fbb);
            ModelProperty.addName(fbb, propertyNameOffset);
            ModelProperty.addType(fbb, type);
        }

        public PropertyBuilder id(int id) {
            checkNotFinished();
            ModelProperty.addId(fbb, id);
            return this;
        }

        public PropertyBuilder indexId(int indexId) {
            checkNotFinished();
            ModelProperty.addIndexId(fbb, indexId);
            return this;
        }

        public PropertyBuilder refId(long refId) {
            checkNotFinished();
            ModelProperty.addRefId(fbb, refId);
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
        Long refId;
        PropertyBuilder propertyBuilder;
        boolean finished;

        EntityBuilder(String name) {
            this.name = name;
        }

        public EntityBuilder id(int id) {
            checkNotFinished();
            this.id = id;
            return this;
        }

        public EntityBuilder refId(long refId) {
            checkNotFinished();
            this.refId = refId;
            return this;
        }

        private void checkNotFinished() {
            if (finished) {
                throw new IllegalStateException("Already finished");
            }
        }

        public PropertyBuilder property(String name, int type) {
            checkNotFinished();
            checkFinishProperty();
            propertyBuilder = new PropertyBuilder(name, type);
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
            if (refId != null) {
                ModelEntity.addRefId(fbb, refId);
            }
            if (id != null) {
                ModelEntity.addId(fbb, id);
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

    public ModelBuilder lastEntityId(long lastEntityId) {
        this.lastEntityId = lastEntityId;
        return this;
    }

    public ModelBuilder lastIndexId(long lastIndexId) {
        this.lastIndexId = lastIndexId;
        return this;
    }

    public byte[] build() {
        int nameOffset = fbb.createString("default");
        int entityVectorOffset = createVector(entityOffsets);
        Model.startModel(fbb);
        Model.addName(fbb, nameOffset);
        Model.addVersion(fbb, 1);
        Model.addEntities(fbb, entityVectorOffset);
        if (lastEntityId != null) {
            Model.addLastEntityId(fbb, lastEntityId);
        }
        if (lastIndexId != null) {
            Model.addLastIndexId(fbb, lastIndexId);
        }
        int offset = Model.endModel(fbb);

        fbb.finish(offset);
        return fbb.sizedByteArray();
    }
}
