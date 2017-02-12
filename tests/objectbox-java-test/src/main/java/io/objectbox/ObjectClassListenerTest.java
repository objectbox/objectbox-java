package io.objectbox;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ObjectClassListenerTest extends AbstractObjectBoxTest {
    @Test
    public void testTwoEntityTypesChanged() {
        store.close();
        store.deleteAllFiles();
        store = createBoxStoreBuilderWithTwoEntities(false).build();

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
                putTestEntities(2);
                boxMini.put(new TestEntityMinimal());
                assertEquals(0, classesWithChanges.size());
            }
        });

        assertEquals(2, classesWithChanges.size());
        assertTrue(classesWithChanges.contains(TestEntity.class));
        assertTrue(classesWithChanges.contains(TestEntityMinimal.class));
    }

}