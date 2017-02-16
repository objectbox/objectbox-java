package io.objectbox;

import org.greenrobot.essentials.collections.MultimapSet;
import org.greenrobot.essentials.collections.MultimapSet.SetType;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import io.objectbox.reactive.Observer;
import io.objectbox.reactive.Publisher;
import io.objectbox.reactive.WeakObserver;

class ObjectClassPublisher implements Publisher<Class> {
    final BoxStore boxStore;
    final MultimapSet<Integer, Observer<Class>> observersByEntityTypeId = MultimapSet.create(SetType.THREAD_SAFE);

    ObjectClassPublisher(BoxStore boxStore) {
        this.boxStore = boxStore;
    }

    @Override
    public void subscribe(Observer<Class> observer, Object forClass) {
        if (forClass == null) {
            for (int entityTypeId : boxStore.getAllEntityTypeIds()) {
                observersByEntityTypeId.putElement(entityTypeId, (Observer) observer);
            }
        } else {
            int entityTypeId = boxStore.getEntityTypeIdOrThrow((Class) forClass);
            observersByEntityTypeId.putElement(entityTypeId, (Observer) observer);
        }
    }


    public void subscribe(ObjectClassObserver observer, Class objectClass) {

    }

    /**
     * Removes the given observer from all object classes it added itself to earlier (forClass == null).
     * This also considers weakly added observers.
     */
    public void unsubscribe(Observer<Class> observer, Object forClass) {
        if (forClass != null) {
            int entityTypeId = boxStore.getEntityTypeIdOrThrow((Class) forClass);
            unsubscribe(observer, entityTypeId);
        } else {
            for (int entityTypeId : boxStore.getAllEntityTypeIds()) {
                unsubscribe(observer, entityTypeId);
            }
        }
    }

    private void unsubscribe(Observer<Class> observer, int entityTypeId) {
        Set<Observer<Class>> observers = observersByEntityTypeId.get(entityTypeId);
        if (observers != null) {
            Iterator<Observer<Class>> iterator = observers.iterator();
            while (iterator.hasNext()) {
                Observer<Class> candidate = iterator.next();
                if (candidate.equals(observer)) {
                    // Unsupported by CopyOnWriteArraySet: iterator.remove();
                    observers.remove(candidate);
                } else if (candidate instanceof WeakObserver) {
                    Observer delegate = ((WeakObserver) candidate).getDelegate();
                    if (delegate == null || delegate.equals(observer)) {
                        // Unsupported by CopyOnWriteArraySet: iterator.remove();
                        observers.remove(candidate);
                    }
                }
            }
        }
        observersByEntityTypeId.removeElement(entityTypeId, observer);
    }

    void publish(int entityTypeId) {
        Collection<Observer<Class>> observers = observersByEntityTypeId.get(entityTypeId);
        if (observers != null) {
            Class objectClass = boxStore.getEntityClassOrThrow(entityTypeId);
            for (Observer<Class> observer : observers) {
                observer.onChange(objectClass);
            }
        }
    }
}
