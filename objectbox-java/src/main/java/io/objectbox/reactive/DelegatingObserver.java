package io.objectbox.reactive;

import io.objectbox.annotation.apihint.Internal;

@Internal
public interface DelegatingObserver<T> {
    DataObserver<T> getObserverDelegate();
}
