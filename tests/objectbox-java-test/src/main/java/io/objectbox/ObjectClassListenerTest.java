package io.objectbox;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ObjectClassListenerTest extends AbstractObjectBoxTest {

    protected BoxStore createBoxStore() {
        return createBoxStoreBuilderWithTwoEntities(false).build();
    }

    @Test
    public void testTwoObjectClassesChanged_catchAllListener() {
        final Box<TestEntityMinimal> boxMini = store.boxFor(TestEntityMinimal.class);
        final List<Class> classesWithChanges = new ArrayList<>();

        store.addObjectClassListener(new ObjectClassListener() {
            @Override
            public void handleChanges(Class objectClass) {
                classesWithChanges.add(objectClass);
            }
        });

        store.runInTx(new Runnable() {
            @Override
            public void run() {
                // Dummy TX, still will be committed
                getTestEntityBox().count();
            }
        });
        assertEquals(0, classesWithChanges.size());

        store.runInTx(new Runnable() {
            @Override
            public void run() {
                putTestEntities(3);
                boxMini.put(new TestEntityMinimal(), new TestEntityMinimal());
                assertEquals(0, classesWithChanges.size());
            }
        });

        assertEquals(2, classesWithChanges.size());
        assertTrue(classesWithChanges.contains(TestEntity.class));
        assertTrue(classesWithChanges.contains(TestEntityMinimal.class));
    }

    @Test
    public void testTwoObjectClassesChanged_oneClassListener() {
        final Box<TestEntityMinimal> boxMini = store.boxFor(TestEntityMinimal.class);
        final List<Class> classesWithChanges = new ArrayList<>();

        store.addObjectClassListener(new ObjectClassListener() {
            @Override
            public void handleChanges(Class objectClass) {
                classesWithChanges.add(objectClass);
            }
        }, TestEntityMinimal.class);

        store.runInTx(new Runnable() {
            @Override
            public void run() {
                putTestEntities(3);
                boxMini.put(new TestEntityMinimal(), new TestEntityMinimal());
                assertEquals(0, classesWithChanges.size());
            }
        });

        assertEquals(1, classesWithChanges.size());
        assertEquals(classesWithChanges.get(0), TestEntityMinimal.class);

        classesWithChanges.clear();
        putTestEntities(1);
        assertEquals(0, classesWithChanges.size());

        boxMini.put(new TestEntityMinimal(), new TestEntityMinimal());
        assertEquals(1, classesWithChanges.size());
    }

}