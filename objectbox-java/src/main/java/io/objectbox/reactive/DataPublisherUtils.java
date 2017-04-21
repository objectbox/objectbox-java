package io.objectbox.reactive;

import java.util.Set;

import io.objectbox.annotation.apihint.Internal;

@Internal
public class DataPublisherUtils {
    /**
     * Observers may be wrapped by @{@link DelegatingObserver}, this will also remove those.
     */
    public static <T> void removeObserverFromCopyOnWriteSet(Set<DataObserver<T>> observers, DataObserver<T> observer) {
        if (observers != null) {
            for (DataObserver<T> candidate : observers) {
                if (candidate.equals(observer)) {
                    // Unsupported by CopyOnWriteArraySet: iterator.remove();
                    observers.remove(candidate);
                } else if (candidate instanceof DelegatingObserver) {
                    DataObserver<T> delegate = candidate;
                    while (delegate instanceof DelegatingObserver) {
                        delegate = ((DelegatingObserver) delegate).getObserverDelegate();
                    }
                    if (delegate == null || delegate.equals(observer)) {
                        // Unsupported by CopyOnWriteArraySet: iterator.remove();
                        observers.remove(candidate);
                    }
                }
            }
        }
    }

}
