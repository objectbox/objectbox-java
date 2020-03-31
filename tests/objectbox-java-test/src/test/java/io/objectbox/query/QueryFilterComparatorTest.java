package io.objectbox.query;

import io.objectbox.TestEntity;
import org.junit.Test;

import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link QueryBuilder#filter(QueryFilter)} and {@link QueryBuilder#sort(Comparator)}.
 */
public class QueryFilterComparatorTest extends AbstractQueryTest {

    private QueryFilter<TestEntity> createTestFilter() {
        return entity -> entity.getSimpleString().contains("e");
    }

    @Test
    public void filter_forEach() {
        putTestEntitiesStrings();
        final StringBuilder stringBuilder = new StringBuilder();
        box.query().filter(createTestFilter()).build()
                .forEach(data -> stringBuilder.append(data.getSimpleString()).append('#'));
        assertEquals("apple#banana milk shake#", stringBuilder.toString());
    }

    @Test
    public void filter_find() {
        putTestEntitiesStrings();
        List<TestEntity> entities = box.query().filter(createTestFilter()).build().find();
        assertEquals(2, entities.size());
        assertEquals("apple", entities.get(0).getSimpleString());
        assertEquals("banana milk shake", entities.get(1).getSimpleString());
    }

    private Comparator<TestEntity> createTestComparator() {
        return Comparator.comparing(o -> o.getSimpleString().substring(1));
    }

    @Test
    public void comparator_find() {
        putTestEntitiesStrings();
        Comparator<TestEntity> testComparator = createTestComparator();
        List<TestEntity> entities = box.query().sort(testComparator).build().find();
        assertEquals(5, entities.size());
        assertEquals("banana", entities.get(0).getSimpleString());
        assertEquals("banana milk shake", entities.get(1).getSimpleString());
        assertEquals("bar", entities.get(2).getSimpleString());
        assertEquals("foo bar", entities.get(3).getSimpleString());
        assertEquals("apple", entities.get(4).getSimpleString());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void filter_count_unsupported() {
        box.query()
                .filter(createTestFilter())
                .build()
                .count();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void filter_remove_unsupported() {
        box.query()
                .filter(createTestFilter())
                .build()
                .remove();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void filter_findFirst_unsupported() {
        box.query()
                .filter(createTestFilter())
                .build()
                .findFirst();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void filter_findUnique_unsupported() {
        box.query()
                .filter(createTestFilter())
                .build()
                .findUnique();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void filter_findOffsetLimit_unsupported() {
        box.query()
                .filter(createTestFilter())
                .build()
                .find(0, 0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void filter_findLazy_unsupported() {
        box.query()
                .filter(createTestFilter())
                .build()
                .findLazy();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void filter_findLazyCached_unsupported() {
        box.query()
                .filter(createTestFilter())
                .build()
                .findLazyCached();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void comparator_forEach_unsupported() {
        box.query()
                .sort(createTestComparator())
                .build()
                .forEach(data -> {
                    // Do nothing.
                });
    }

    @Test(expected = UnsupportedOperationException.class)
    public void comparator_findFirst_unsupported() {
        box.query()
                .sort(createTestComparator())
                .build()
                .findFirst();
    }

    @Test
    public void comparator_findUnique_supported() {
        box.query()
                .sort(createTestComparator())
                .build()
                .findUnique();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void comparator_findOffsetLimit_unsupported() {
        box.query()
                .sort(createTestComparator())
                .build()
                .find(0, 0);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void comparator_findLazy_unsupported() {
        box.query()
                .sort(createTestComparator())
                .build()
                .findLazy();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void comparator_findLazyCached_unsupported() {
        box.query()
                .sort(createTestComparator())
                .build()
                .findLazyCached();
    }

}
