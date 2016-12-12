package io.objectbox;

import org.junit.Test;

public class TransactionPerfTest extends AbstractObjectBoxTest {

    @Test
    public void testBoxReadTxPerformance() {
        Box<TestEntity> box = getTestEntityBox();
        TestEntity entity = new TestEntity();
        entity.setSimpleString("foobar");
        long id = box.put(entity);
        long start = System.currentTimeMillis();
        int count = 100000;
        for (int i = 0; i < count; i++) {
            box.get(id);
        }
        long time = System.currentTimeMillis() - start;
        log("Read with box: " + valuesPerSec(count, time));
    }

    @Test
    public void testCloseReadTxPerformance() {
        Box<TestEntity> box = getTestEntityBox();
        TestEntity entity = new TestEntity();
        entity.setSimpleString("foobar");
        long id = box.put(entity);
        long start = System.currentTimeMillis();
        int count = 100000;
        try {
            for (int i = 0; i < count; i++) {
                Transaction tx = store.sharedReadTx();
                box.get(id);
                // Use with release build to prevent additional log here
                tx.reset();
            }
        } finally {
            log(store.diagnose());
        }
        long time = System.currentTimeMillis() - start;
        log("Read with box and reset TX: " + valuesPerSec(count, time));
    }

    @Test
    public void testExplicitTxPerformance() {
        final Box<TestEntity> box = getTestEntityBox();
        store.runInTx(new Runnable() {
            @Override
            public void run() {
                TestEntity entity = new TestEntity();
                entity.setSimpleString("foobar");
                long id = box.put(entity);
                long start = System.currentTimeMillis();
                int count = 100000;
                for (int i = 0; i < count; i++) {
                    box.get(id);
                }
                long time = System.currentTimeMillis() - start;
                log("Read with box inside write TX: " + valuesPerSec(count, time));
            }
        });
    }

    private String valuesPerSec(int count, long timeMillis) {
        return count + " in " + timeMillis + ": " + (timeMillis > 0 ? (count * 1000 / timeMillis) : "N/A") + " values/s";
    }

}
