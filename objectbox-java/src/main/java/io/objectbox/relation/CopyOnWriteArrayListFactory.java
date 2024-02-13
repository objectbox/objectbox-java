package io.objectbox.relation;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class CopyOnWriteArrayListFactory extends ListFactory {

    CopyOnWriteArrayListFactory() {
        super(1888039726372206411L);
    }

    @Override
    public <T> List<T> createList() {
        return new CopyOnWriteArrayList<>();
    }
}