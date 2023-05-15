package io.objectbox.query;

import java.util.Arrays;
import java.util.List;

import io.objectbox.Property;
import io.objectbox.TestEntity;
import org.junit.Test;

import static io.objectbox.TestEntity_.charArray;
import static io.objectbox.TestEntity_.doubleArray;
import static io.objectbox.TestEntity_.floatArray;
import static io.objectbox.TestEntity_.intArray;
import static io.objectbox.TestEntity_.longArray;
import static io.objectbox.TestEntity_.shortArray;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

/**
 * Tests querying properties that are integer or floating point arrays.
 */
public class QueryScalarVectorTest extends AbstractQueryTest {

    /**
     * Note: byte array is tested separately in {@link QueryTest}.
     */
    @Test
    public void integer() {
        List<TestEntity> entities = putTestEntitiesScalars();
        List<Property<TestEntity>> properties = Arrays.asList(
                shortArray,
                intArray,
                longArray
        );
        long[] ids = entities.stream().mapToLong(TestEntity::getId).toArray();

        long id5 = ids[4];
        List<Long> params5 = Arrays.asList(
                2104L, // short
                2004L, // int
                3004L // long
        );
        long[] id6To10 = Arrays.stream(ids).filter(value -> value > id5).toArray();

        long id10 = ids[9];
        List<Long> params10 = Arrays.asList(
                2109L, // short
                2009L, // int
                3009L // long
        );

        for (int i = 0; i < properties.size(); i++) {
            Property<TestEntity> property = properties.get(i);
            Long param5 = params5.get(i);
            Long param10 = params10.get(i);

            // "greater" which behaves like "has element greater".
            try (Query<TestEntity> query = box.query()
                    .greater(property, 3010)
                    .build()) {
                assertEquals(0, query.findUniqueId());

                query.setParameter(property, param5);
                assertArrayEquals(id6To10, query.findIds());
            }
            // "greater or equal", only check equal
            try (Query<TestEntity> query = box.query()
                    .greaterOrEqual(property, param10)
                    .build()) {
                assertEquals(id10, query.findUniqueId());
            }

            // "less" which behaves like "has element less".
            try (Query<TestEntity> query = box.query()
                    .less(property, -3010)
                    .build()) {
                assertEquals(0, query.findUniqueId());

                query.setParameter(property, -param5);
                assertArrayEquals(id6To10, query.findIds());
            }
            // "less or equal", only check equal
            try (Query<TestEntity> query = box.query()
                    .lessOrEqual(property, -param10)
                    .build()) {
                assertEquals(id10, query.findUniqueId());
            }

            // Note: "equal" for scalar arrays is actually "contains element".
            try (Query<TestEntity> query = box.query()
                    .equal(property, -1)
                    .build()) {
                assertEquals(0, query.findUniqueId());

                query.setParameter(property, param5);
                assertEquals(id5, query.findUniqueId());
            }

            // Note: "not equal" for scalar arrays does not do anything useful.
            try (Query<TestEntity> query = box.query()
                    .notEqual(property, param5)
                    .build()) {
                assertArrayEquals(ids, query.findIds());
            }
        }

    }

    @Test
    public void charArray() {
        List<TestEntity> entities = putTestEntitiesStrings();
        long[] ids = entities.stream().mapToLong(TestEntity::getId).toArray();

        Property<TestEntity> property = charArray;
        long id2 = entities.get(1).getId();
        long id4 = entities.get(3).getId();
        long[] id3to5 = Arrays.stream(ids).filter(value -> value > id2).toArray();

        // "greater" which behaves like "has element greater".
        try (Query<TestEntity> query = box.query()
                .greater(property, 'x')
                .build()) {
            assertEquals(0, query.findUniqueId());

            query.setParameter(property, 'p' /* apple */);
            assertArrayEquals(id3to5, query.findIds());
        }
        // "greater or equal", only check equal
        try (Query<TestEntity> query = box.query()
                .greaterOrEqual(property, 's' /* banana milk shake */)
                .build()) {
            assertEquals(id4, query.findUniqueId());
        }

        // "less" which behaves like "has element less".
        long[] id4And5 = new long[]{ids[3], ids[4]};
        try (Query<TestEntity> query = box.query()
                .less(property, ' ')
                .build()) {
            assertEquals(0, query.findUniqueId());

            query.setParameter(property, 'a');
            assertArrayEquals(id4And5, query.findIds());
        }
        // "less or equal", only check equal
        try (Query<TestEntity> query = box.query()
                .lessOrEqual(property, ' ')
                .build()) {
            assertArrayEquals(id4And5, query.findIds());
        }

        // Note: "equal" for scalar arrays is actually "contains element".
        try (Query<TestEntity> query = box.query()
                .equal(property, 'x')
                .build()) {
            assertEquals(0, query.findUniqueId());
            
            query.setParameter(property, 'p' /* apple */);
            assertEquals(id2, query.findUniqueId());
        }

        // Note: "not equal" for scalar arrays does not do anything useful.
        try (Query<TestEntity> query = box.query()
                .notEqual(property, 'p' /* apple */)
                .build()) {
            assertArrayEquals(
                    entities.stream().mapToLong(TestEntity::getId).toArray(),
                    query.findIds()
            );
        }
    }

    @Test
    public void floatingPoint() {
        List<TestEntity> entities = putTestEntitiesScalars();
        List<Property<TestEntity>> properties = Arrays.asList(
                floatArray,
                doubleArray
        );
        long[] ids = entities.stream().mapToLong(TestEntity::getId).toArray();

        long id5 = ids[4];
        List<Double> params5 = Arrays.asList(
                400.4, // float
                (double) (2000 + 2004 / 100f) // double
        );
        long[] id6To10 = Arrays.stream(ids).filter(value -> value > id5).toArray();

        long id10 = ids[9];
        List<Double> params10 = Arrays.asList(
                400.9, // float
                (double) (2000 + 2009 / 100f) // double
        );

        for (int i = 0; i < properties.size(); i++) {
            Property<TestEntity> property = properties.get(i);
            System.out.println(property);
            Double param5 = params5.get(i);
            Double param10 = params10.get(i);

            // "greater" which behaves like "has element greater".
            try (Query<TestEntity> query = box.query()
                    .greater(property, 2021.0)
                    .build()) {
                assertEquals(0, query.findUniqueId());

                query.setParameter(property, param5);
                assertArrayEquals(id6To10, query.findIds());
            }
            // "greater or equal", only check equal
            try (Query<TestEntity> query = box.query()
                    .greaterOrEqual(property, param10)
                    .build()) {
                assertEquals(id10, query.findUniqueId());
            }

            // "less" which behaves like "has element less".
            try (Query<TestEntity> query = box.query()
                    .less(property, -param5)
                    .build()) {
                assertArrayEquals(id6To10, query.findIds());
            }
            // "less or equal", only check equal
            try (Query<TestEntity> query = box.query()
                    .lessOrEqual(property, -2021.0)
                    .build()) {
                assertEquals(0, query.findUniqueId());

                query.setParameter(property, -param10);
                assertEquals(id10, query.findUniqueId());
            }

            // "equal" which is actually "between" for floating point, is not supported.
            assertThrows(IllegalArgumentException.class, () -> box.query().equal(property, param5, 0));
        }
    }

}
