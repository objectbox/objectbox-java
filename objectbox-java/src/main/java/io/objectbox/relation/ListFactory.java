package io.objectbox.relation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.objectbox.annotation.apihint.Experimental;

@Experimental
public interface ListFactory {
    <T> List<T> createList();

    class ArrayListFactory implements ListFactory {

        @Override
        public <T> List<T> createList() {
            return new ArrayList<>();
        }
    }

    class CopyOnWriteArrayListFactory implements ListFactory {

        @Override
        public <T> List<T> createList() {
            return new CopyOnWriteArrayList<>();
        }
    }
}
