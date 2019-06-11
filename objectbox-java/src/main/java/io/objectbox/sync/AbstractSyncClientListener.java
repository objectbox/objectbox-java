package io.objectbox.sync;

/**
 * Abstract implementation for {@link SyncClientListener} providing empty methods doing nothing ("adapter class").
 * Helpful if you do not want to implement all methods.
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
