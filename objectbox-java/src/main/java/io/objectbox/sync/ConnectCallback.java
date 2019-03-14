package io.objectbox.sync;

import javax.annotation.Nullable;

public interface ConnectCallback {

    void onComplete(@Nullable Throwable throwable);

}
