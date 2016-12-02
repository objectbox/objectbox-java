package io.objectbox.internal;

import io.objectbox.annotation.apihint.Internal;

@Internal
public interface CallWithHandle<RESULT> {
    RESULT call(long handle);
}
