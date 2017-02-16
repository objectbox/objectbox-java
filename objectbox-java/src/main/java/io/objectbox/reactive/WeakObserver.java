package io.objectbox.reactive;

import java.lang.ref.WeakReference;

import io.objectbox.annotation.apihint.Internal;

@Internal
public class WeakObserver<T> implements Observer<T> {
    private final WeakReference<Observer<T>> weakDelegate;
    private Subscription subscription;

    WeakObserver(Observer<T> delegate) {
        this.weakDelegate = new WeakReference<>(delegate);
    }

    @Override
    public void onChange(T data) {
        Observer<T> delegate = weakDelegate.get();
        if (delegate != null) {
            delegate.onChange(data);
        } else {
            subscription.cancel();
        }
    }

    public Observer<T> getDelegate() {
        return weakDelegate.get();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof WeakObserver) {
            Observer<T> delegate = weakDelegate.get();
            if (delegate != null && delegate == ((WeakObserver) other).weakDelegate.get()) {
                return true;
            }
            return super.equals(other);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        Observer<T> delegate = weakDelegate.get();
        if (delegate != null) {
            return delegate.hashCode();
        } else {
            return super.hashCode();
        }
    }

    public void setSubscription(Subscription subscription) {
        this.subscription = subscription;
    }

}
