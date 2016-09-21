package io.objectbox;

import java.io.File;

public class BoxStoreBuilder {
    final File dbFile;
    final byte[] model;

    long modelVersion = 1;
    long maxSizeInKByte = 100 * 1024;

    ModelUpdate modelUpdate;

    public BoxStoreBuilder(File dbFile, byte[] model) {
        this.model = model;
        if (dbFile == null) {
            throw new IllegalArgumentException("DB file may not be null");
        }
        if (model == null) {
            throw new IllegalArgumentException("Model may not be null");
        }
        this.dbFile = dbFile;
    }

    public BoxStoreBuilder(String dbName, byte[] model) {
        this(new File(dbName), model);
    }

    public BoxStoreBuilder modelVersion(long modelVersion) {
        this.modelVersion = modelVersion;
        return this;
    }

    public BoxStoreBuilder modelUpdate(ModelUpdate modelUpdate) {
        this.modelUpdate = modelUpdate;
        return this;
    }

    public BoxStoreBuilder maxSizeInKByte(long maxSizeInKByte) {
        this.maxSizeInKByte = maxSizeInKByte;
        return this;
    }

    public BoxStore build() {
        return new BoxStore(this);
    }

}
