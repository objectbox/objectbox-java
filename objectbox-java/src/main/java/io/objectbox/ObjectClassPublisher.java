package io.objectbox;

import org.greenrobot.essentials.collections.MultimapSet;
import org.greenrobot.essentials.collections.MultimapSet.SetType;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import io.objectbox.annotation.apihint.Internal;
import io.objectbox.reactive.DataObserver;
import io.objectbox.reactive.DataPublisher;
import io.objectbox.reactive.WeakDataObserver;

@Internal
class ObjectClassPublisher implements DataPublisher<Class> {
    final BoxStore boxStore;
    final MultimapSet<Integer, DataObserver<Class>> observersByEntityTypeId = MultimapSet.create(SetType.THREAD_SAFE);

    ObjectClassPublisher(BoxStore boxStore) {
        this.boxStore = boxStore;
    }

    @Override
    public void subscribe(DataObserver<Class> observer, Object forClass) {
        if (forClass == null) {
            for (int entityTypeId : boxStore.getAllEntityTypeIds()) {
                observersByEntityTypeId.putElement(entityTypeId, (DataObserver) observer);
            }
        } else {
            int entityTypeId = boxStore.getEntityTypeIdOrThrow((Class) forClass);
            observersByEntityTypeId.putElement(entityTypeId, (DataObserver) observer);
        }
    }

    /**
     * Removes the given observer from all object classes it added itself to earlier (forClass == null).
     * This also considers weakly added observers.
     */
    public void unsubscribe(DataObserver<Class> observer, Object forClass) {
        if (forClass != null) {
            int entityTypeId = boxStore.getEntityTypeIdOrThrow((Class) forClass);
            unsubscribe(observer, entityTypeId);
        } else {
            for (int entityTypeId : boxStore.getAllEntityTypeIds()) {
                unsubscribe(observer, entityTypeId);
            }
        }
    }

    private void unsubscribe(DataObserver<Class> observer, int entityTypeId) {
        Set<DataObserver<Class>> observers = observersByEntityTypeId.get(entityTypeId);
        if (observers != null) {
            Iterator<DataObserver<Class>> iterator = observers.iterator();
            while (iterator.hasNext()) {
                DataObserver<Class> candidate = iterator.next();
                if (candidate.equals(observer)) {
                    // Unsupported by CopyOnWriteArraySet: iterator.remove();
                    observers.remove(candidate);
                } else if (candidate instanceof WeakDataObserver) {
                    DataObserver delegate = ((WeakDataObserver) candidate).getDelegate();
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
        Collection<DataObserver<Class>> observers = observersByEntityTypeId.get(entityTypeId);
        if (observers != null) {
            Class objectClass = boxStore.getEntityClassOrThrow(entityTypeId);
            for (DataObserver<Class> observer : observers) {
                observer.onData(objectClass);
            }
        }
    }
}
