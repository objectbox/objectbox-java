package io.objectbox;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ObjectClassListenerTest extends AbstractObjectBoxTest {

    protected BoxStore createBoxStore() {
        return createBoxStoreBuilderWithTwoEntities(false).build();
    }

    final List<Class> classesWithChanges = new ArrayList<>();
    ObjectClassListener objectClassListener = new ObjectClassListener() {
        @Override
        public void handleChanges(Class objectClass) {
            classesWithChanges.add(objectClass);
        }
    };

    Runnable txRunnable = new Runnable() {
        @Override
        public void run() {
            putTestEntities(3);
            Box<TestEntityMinimal> boxMini = store.boxFor(TestEntityMinimal.class);
            boxMini.put(new TestEntityMinimal(), new TestEntityMinimal());
            assertEquals(0, classesWithChanges.size());
        }
    };

    @Before
    public void clear() {
        classesWithChanges.clear();
    }

    @Test
    public void testTwoObjectClassesChanged_catchAllListener() {
        store.addObjectClassListener(objectClassListener);
        store.runInTx(new Runnable() {
            @Override
            public void run() {
                // Dummy TX, still will be committed
                getTestEntityBox().count();
            }
        });
        assertEquals(0, classesWithChanges.size());

        store.runInTx(txRunnable);
        assertEquals(2, classesWithChanges.size());
        assertTrue(classesWithChanges.contains(TestEntity.class));
        assertTrue(classesWithChanges.contains(TestEntityMinimal.class));

        classesWithChanges.clear();
        store.removeObjectClassListener(objectClassListener);
        store.runInTx(txRunnable);
        assertEquals(0, classesWithChanges.size());
    }

    @Test
    public void testTwoObjectClassesChanged_oneClassListener() {
        store.addObjectClassListener(objectClassListener, TestEntityMinimal.class);
        store.runInTx(txRunnable);

        assertEquals(1, classesWithChanges.size());
        assertEquals(classesWithChanges.get(0), TestEntityMinimal.class);

        classesWithChanges.clear();
        putTestEntities(1);
        assertEquals(0, classesWithChanges.size());

        Box<TestEntityMinimal> boxMini = store.boxFor(TestEntityMinimal.class);
        boxMini.put(new TestEntityMinimal(), new TestEntityMinimal());
        assertEquals(1, classesWithChanges.size());

        classesWithChanges.clear();
        store.removeObjectClassListener(objectClassListener);
        store.runInTx(txRunnable);
        assertEquals(0, classesWithChanges.size());
    }

}