package io.objectbox;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;


import io.objectbox.query.Query;


import static io.objectbox.TestEntityProperties.SimpleInt;
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
    public void testQueryConditions() {
        putTestEntities();

        Query<TestEntity> query = box.query().equal(SimpleInt, 2007).build();
        assertEquals(8, query.findFirst().getId());
        assertEquals(8, query.findUnique().getId());
        List<TestEntity> all = query.findAll();
        assertEquals(1, all.size());
        assertEquals(8, all.get(0).getId());
    }

    @Test
    public void testNoConditions() {
        List<TestEntity> entities = putTestEntities();
        Query<TestEntity> query = box.query().build();
        List<TestEntity> all = query.findAll();
        assertEquals(entities.size(), all.size());
    }

    private List<TestEntity> putTestEntities() {
        List<TestEntity> entities = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            TestEntity entity = new TestEntity();
            entity.setSimpleInt(2000 + i);
            entities.add(entity);
        }
        box.put(entities);
        return entities;
    }

}
