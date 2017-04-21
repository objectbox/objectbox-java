package io.objectbox.reactive;

import javax.annotation.Nullable;

import io.objectbox.annotation.apihint.Internal;

@Internal
public interface DataPublisher<T> {
    void subscribe(DataObserver<T> observer, @Nullable Object param);

    void publishSingle(DataObserver<T> observer, @Nullable Object param);

    void unsubscribe(DataObserver<T> observer, @Nullable Object param);
}
