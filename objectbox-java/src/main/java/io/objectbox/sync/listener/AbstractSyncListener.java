package io.objectbox.sync.listener;

import io.objectbox.annotation.apihint.Experimental;
import io.objectbox.sync.SyncChange;

/**
 * A {@link SyncListener} with empty implementations of all interface methods.
 * This is helpful if you only want to override some methods.
 */
@Experimental
public abstract class AbstractSyncListener implements SyncListener {

    @Override
    public void onLoggedIn() {
    }

    @Override
    public void onLoginFailed(long syncLoginCode) {
    }

    @Override
    public void onUpdatesCompleted() {
    }

    @Override
    public void onSyncChanges(SyncChange[] syncChanges) {
    }

    @Override
    public void onDisconnected() {
    }
}
