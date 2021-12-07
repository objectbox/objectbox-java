package io.objectbox

import io.objectbox.kotlin.awaitCallInTx
import io.objectbox.kotlin.boxFor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test


class BoxStoreTestK : AbstractObjectBoxTest() {

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

    @ExperimentalCoroutinesApi
    @Test
    fun awaitCallInTx() {
        val box = store.boxFor<TestEntity>()
        runTest {
            // put
            val id = store.awaitCallInTx {
                box.put(createTestEntity("Hello", 1))
            }
            assertEquals(1, id!!)

            // get
            val note = store.awaitCallInTx {
                box.get(id)
            }
            assertEquals("Hello", note!!.simpleString)
        }
    }
}