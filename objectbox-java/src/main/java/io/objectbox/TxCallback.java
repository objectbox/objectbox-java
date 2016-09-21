package io.objectbox;

/**
 * Created by markus.
 */
public interface TxCallback<T> {
    void txFinished(T result, Throwable error);
}
