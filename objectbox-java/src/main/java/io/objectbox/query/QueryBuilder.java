package io.objectbox.query;

import io.objectbox.Box;
import io.objectbox.Property;
import io.objectbox.annotation.apihint.Internal;

/**
 * Created by Markus on 13.10.2016.
 */
public class QueryBuilder<T> {
    private final Box<T> box;

    private long handle;

    private static native long nativeCreate(long storeHandle, String entityName);

    private static native long nativeDestroy(long handle);

    private static native long nativeBuild(long handle);

    private static native void nativeEqual(long handle, int propertyId, long value);

    @Internal
    public QueryBuilder(Box<T> box, long storeHandle, String entityName) {
        this.box = box;

        // This ensures that all properties have been set
        box.getProperties();

        handle = nativeCreate(storeHandle, entityName);
    }

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    public void close() {
        if (handle != 0) {
            nativeDestroy(handle);
            handle = 0;
        }
    }

    /**
     * Builds the query and closes this QueryBuilder.
     */
    public Query<T> build() {
        if (handle == 0) {
            throw new IllegalStateException("This QueryBuilder has already been closed. Please use a new instance.");
        }
        long queryHandle = nativeBuild(handle);
        Query<T> query = new Query<T>(box, queryHandle);
        close();
        return query;
    }

    public QueryBuilder<T> equal(Property property, long value) {
        nativeEqual(handle, property.getId(), value);
        return this;
    }
}
