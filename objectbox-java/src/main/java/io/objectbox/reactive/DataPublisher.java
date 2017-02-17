package io.objectbox.reactive;

import io.objectbox.annotation.apihint.Internal;

@Internal
public interface DataPublisher<T> {
    void subscribe(DataObserver<T> observer, Object param);

    void unsubscribe(DataObserver<T> observer, Object param);
}
