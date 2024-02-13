package io.objectbox.relation;

import java.util.ArrayList;
import java.util.List;

class ArrayListFactory extends ListFactory {

    ArrayListFactory() {
        super(8247662514375611729L);
    }

    @Override
    public <T> List<T> createList() {
        return new ArrayList<>();
    }
}
