package io.objectbox.query;

import io.objectbox.TestEntity;
import io.objectbox.TestEntity_;
import org.junit.Test;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class QueryCopyTest extends AbstractQueryTest {

    @Test
    public void queryCopy_isClone() {
        putTestEntity("orange", 1);
        TestEntity banana = putTestEntity("banana", 2);
        putTestEntity("apple", 3);
        TestEntity bananaMilkShake = putTestEntity("banana milk shake", 4);
        putTestEntity("pineapple", 5);
        putTestEntity("papaya", 6);

        // Only even nr: 2, 4, 6.
        QueryFilter<TestEntity> filter = entity -> entity.getSimpleInt() % 2 == 0;
        // Reverse insert order: 6, 4, 2.
        Comparator<TestEntity> comparator = Comparator.comparingLong(testEntity -> -testEntity.getId());

        Query<TestEntity> queryOriginal = box.query(TestEntity_.simpleString.contains("").alias("fruit"))
                .filter(filter)
                .sort(comparator)
                .build();
        // Only bananas: 4, 2.
        queryOriginal.setParameter("fruit", banana.getSimpleString());

        Query<TestEntity> queryCopy = queryOriginal.copy();

        // Object instances and native query handle should differ.
        assertNotEquals(queryOriginal, queryCopy);
        assertNotEquals(queryOriginal.handle, queryCopy.handle);

        // Verify results are identical.
        List<TestEntity> resultsOriginal = queryOriginal.find();
        queryOriginal.close();
        List<TestEntity> resultsCopy = queryCopy.find();
        queryCopy.close();
        assertEquals(2, resultsOriginal.size());
        assertEquals(2, resultsCopy.size());
        assertTestEntityEquals(bananaMilkShake, resultsOriginal.get(0));
        assertTestEntityEquals(bananaMilkShake, resultsCopy.get(0));
        assertTestEntityEquals(banana, resultsOriginal.get(1));
        assertTestEntityEquals(banana, resultsCopy.get(1));
    }

    @Test
    public void queryCopy_setParameter_noEffectOriginal() {
        TestEntity orange = putTestEntity("orange", 1);
        TestEntity banana = putTestEntity("banana", 2);

        Query<TestEntity> queryOriginal = box
                .query(TestEntity_.simpleString.equal(orange.getSimpleString()).alias("fruit"))
                .build();

        // Set parameter on clone that changes result.
        Query<TestEntity> queryCopy = queryOriginal.copy()
                .setParameter("fruit", banana.getSimpleString());

        List<TestEntity> resultsOriginal = queryOriginal.find();
        queryOriginal.close();
        assertEquals(1, resultsOriginal.size());
        assertTestEntityEquals(orange, resultsOriginal.get(0));

        List<TestEntity> resultsCopy = queryCopy.find();
        queryCopy.close();
        assertEquals(1, resultsCopy.size());
        assertTestEntityEquals(banana, resultsCopy.get(0));
    }

    private void assertTestEntityEquals(TestEntity expected, TestEntity actual) {
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getSimpleString(), actual.getSimpleString());
    }

    @Test
    public void queryThreadLocal() throws InterruptedException {
        Query<TestEntity> queryOriginal = box.query().build();
        QueryThreadLocal<TestEntity> threadLocal = new QueryThreadLocal<>(queryOriginal);

        AtomicReference<Query<TestEntity>> queryThreadAtomic = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        new Thread(() -> {
            queryThreadAtomic.set(threadLocal.get());
            latch.countDown();
        }).start();

        assertTrue(latch.await(1, TimeUnit.SECONDS));

        Query<TestEntity> queryThread = queryThreadAtomic.get();
        Query<TestEntity> queryMain = threadLocal.get();

        // Assert that initialValue returns something.
        assertNotNull(queryThread);
        assertNotNull(queryMain);

        // Assert that initialValue returns clones.
        assertNotEquals(queryThread.handle, queryOriginal.handle);
        assertNotEquals(queryMain.handle, queryOriginal.handle);
        assertNotEquals(queryThread.handle, queryMain.handle);

        queryOriginal.close();
        queryMain.close();
        queryThread.close();
    }
}
