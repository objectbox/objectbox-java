package io.objectbox.query;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import io.objectbox.AbstractObjectBoxTest;
import io.objectbox.Box;
import io.objectbox.TestEntity;
import io.objectbox.reactive.DataObserver;


import static io.objectbox.TestEntity_.simpleInt;
import static org.junit.Assert.assertEquals;

public class QueryObserverTest extends AbstractObjectBoxTest implements DataObserver<List<TestEntity>> {

    private Box<TestEntity> box;
    private List<List<TestEntity>> receivedChanges = new CopyOnWriteArrayList<>();
    private CountDownLatch latch = new CountDownLatch(1);

    @Before
    public void setUpBox() {
        box = getTestEntityBox();
    }

    @Test
    public void testObserver() {
        int[] valuesInt = {2003, 2007, 2002};
        Query<TestEntity> query = box.query().in(simpleInt, valuesInt).build();
        assertEquals(0, query.count());

        query.subscribe().observer(this);
        putTestEntitiesScalars();
        assertLatchCountedDown(latch, 5);

        assertEquals(1, receivedChanges.size());
        assertEquals(3, receivedChanges.get(0).size());
    }


    private List<TestEntity> putTestEntitiesScalars() {
        return putTestEntities(10, null, 2000);
    }

    @Override
    public void onData(List<TestEntity> queryResult) {
        receivedChanges.add(queryResult);
        latch.countDown();
    }
}
