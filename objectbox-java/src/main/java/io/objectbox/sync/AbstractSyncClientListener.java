package io.objectbox.sync;

/**
 * A {@link SyncClientListener} with empty implementations of all interface methods.
 * This is helpful if you only want to override some methods.
 */
public abstract class AbstractSyncClientListener implements SyncClientListener {
    @Override
    public void onLogin() {
    }

    @Override
    public void onLoginFailure(long response) {
    }

    @Override
    public void onSyncComplete() {
    }

    @Override
    public void onDisconnect() {
    }
}
