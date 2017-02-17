package io.objectbox.reactive;

import io.objectbox.annotation.apihint.Internal;

@Internal
public interface RunWithParam<T> {
    void run(T param);
}
