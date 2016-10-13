package io.objectbox;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;


import io.objectbox.query.Query;


import static io.objectbox.TestEntityProperties.SimpleInt;
import static io.objectbox.TestEntityProperties.SimpleShort;
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
    public void testScalarEqual() {
        putTestEntities();

        Query<TestEntity> query = box.query().equal(SimpleInt, 2007).build();
        assertEquals(1, query.count());
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
        assertEquals(entities.size(), query.count());
    }

    @Test
    public void testScalarNotEqual() {
        List<TestEntity> entities = putTestEntities();
        Query<TestEntity> query = box.query().notEqual(SimpleInt, 2007).notEqual(SimpleInt, 2002).build();
        assertEquals(entities.size()-2, query.count());
    }

    @Test
    public void testScalarLessAndGreater() {
        putTestEntities();
        Query<TestEntity> query = box.query().greater(SimpleInt, 2003).less(SimpleShort, 207).build();
        assertEquals(3, query.count());
    }

    private List<TestEntity> putTestEntities() {
        List<TestEntity> entities = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            TestEntity entity = new TestEntity();
            entity.setSimpleInt(2000 + i);
            entity.setSimpleShort((short) (200 + i));
            entities.add(entity);
        }
        box.put(entities);
        return entities;
    }

}
