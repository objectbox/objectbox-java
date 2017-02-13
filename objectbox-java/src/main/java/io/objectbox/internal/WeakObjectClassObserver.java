package io.objectbox.internal;

import java.lang.ref.WeakReference;

import io.objectbox.BoxStore;
import io.objectbox.ObjectClassObserver;
import io.objectbox.annotation.apihint.Internal;

@Internal
public class WeakObjectClassObserver implements ObjectClassObserver {
    private final BoxStore boxStore;
    private final WeakReference<ObjectClassObserver> weakDelegate;

    public WeakObjectClassObserver(BoxStore boxStore, ObjectClassObserver delegate) {
        this.boxStore = boxStore;
        this.weakDelegate = new WeakReference<>(delegate);
    }

    @Override
    public void onChanges(Class objectClass) {
        ObjectClassObserver delegate = weakDelegate.get();
        if(delegate != null) {
            delegate.onChanges(objectClass);
        } else {
            boxStore.unsubscribe(this);
        }
    }

    public ObjectClassObserver getDelegate() {
        return weakDelegate.get();
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof  WeakObjectClassObserver) {
            ObjectClassObserver delegate = weakDelegate.get();
            if(delegate != null && delegate == ((WeakObjectClassObserver) other).weakDelegate.get()) {
                return true;
            }
            return super.equals(other);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        ObjectClassObserver delegate = weakDelegate.get();
        if(delegate != null) {
            return delegate.hashCode();
        } else {
            return super.hashCode();
        }
    }
}
