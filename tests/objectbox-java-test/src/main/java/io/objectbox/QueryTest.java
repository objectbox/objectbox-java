package io.objectbox;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


import io.objectbox.query.Query;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class QueryTest extends AbstractObjectBoxTest {

    private Box<TestEntity> box;

    @Before
    public void setUpBox() {
        box = getTestEntityBox();
    }

    @Test
    public void testBuild() {
        Query query = box.query().build();
        assertNotNull(query);
    }

    @Test
    public void testTodo() {
        List<TestEntity> entities = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            TestEntity entity = new TestEntity();
            entity.setSimpleInt(2000 + i);
            entities.add(entity);
        }
        box.put(entities);
        assertEquals(entities.size(), box.count());

    }

}
