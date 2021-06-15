package io.objectbox

import io.objectbox.kotlin.boxFor
import org.junit.Assert.assertEquals
import org.junit.Test


class BoxStoreTestK: AbstractObjectBoxTest() {

    /**
     * This is mostly to test the expected syntax works without errors or warnings.
     */
    @Test
    fun boxFor() {
        val boxJavaApi = testEntityBox
        val box = store.boxFor<TestEntity>()
        assertEquals(boxJavaApi, box)

        // Note the difference to Java API: TestEntity::class.java
        val box2 = store.boxFor(TestEntity::class)
        assertEquals(boxJavaApi, box2)
    }
}