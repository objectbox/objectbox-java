package io.objectbox.relation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.objectbox.annotation.apihint.Experimental;

@Experimental
public interface ListFactory extends Serializable {
    <T> List<T> createList();

    class ArrayListFactory implements ListFactory {
        private static final long serialVersionUID = 8247662514375611729L;

        @Override
        public <T> List<T> createList() {
            return new ArrayList<>();
        }
    }

    class CopyOnWriteArrayListFactory implements ListFactory {
        private static final long serialVersionUID = 1888039726372206411L;

        @Override
        public <T> List<T> createList() {
            return new CopyOnWriteArrayList<>();
        }
    }
}
