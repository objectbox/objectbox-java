package io.objectbox

import io.objectbox.kotlin.awaitCallInTx
import io.objectbox.kotlin.boxFor
import io.objectbox.kotlin.newCachedThreadPoolDispatcher
import io.objectbox.kotlin.newFixedThreadPoolDispatcher
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.Executors


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

    /**
     * This appears to duplicate [ObjectBoxThreadPoolExecutorTest.executor_cleansThreadResources], however,
     * this makes sure that a coroutine continues to be executed like a runnable and clean up of
     * [ObjectBoxThreadPoolExecutor] continues to work in this case.
     */
    @Test
    fun dispatcherBackedByObjectBoxExecutor_cleansThreadResources() {
        runTest {
            // Use a single thread to make it easy to check thread-local Box resources have been cleaned up
            val dispatcher = store.newFixedThreadPoolDispatcher(1)

            val hasReaderCursor = withContext(dispatcher) {
                val box = store.boxFor<TestEntity>()
                val entity = createTestEntity("dispatcher-test", 1)
                val id = box.put(entity)
                box.get(id)
                box.hasReaderCursorForCurrentThread()
            }
            // Verify that a thread-local reader cursor was created
            assertTrue(hasReaderCursor)

            // Check the thread-local reader cursor was released after the previous coroutine was executed
            val hasReaderCursorAfter = withContext(dispatcher) {
                store.boxFor<TestEntity>().hasReaderCursorForCurrentThread()
            }
            assertFalse(hasReaderCursorAfter)
        }
    }

    @Test
    fun newCachedThreadPoolDispatcher_works() {
        runTest {
            assertDispatcher(store.newCachedThreadPoolDispatcher())
            assertDispatcher(store.newCachedThreadPoolDispatcher(Executors.defaultThreadFactory()))
        }
    }

    @Test
    fun newFixedThreadPoolDispatcher_works() {
        runTest {
            assertDispatcher(store.newFixedThreadPoolDispatcher(2))
            assertDispatcher(store.newFixedThreadPoolDispatcher(2, Executors.defaultThreadFactory()))
        }
    }

    /**
     * Quickly checks the pre-configured dispatchers work.
     */
    private suspend fun assertDispatcher(dispatcher: ExecutorCoroutineDispatcher) {
        dispatcher.use { dispatcher ->
            // Create at least a write and a read transaction
            val testEntity: TestEntity? = withContext(dispatcher) {
                val box = store.boxFor<TestEntity>()
                val entity = createTestEntity("dispatcher-test", 1)
                val id = box.put(entity)

                box.get(id)
            }
            assertEquals("dispatcher-test", testEntity!!.simpleString)
        }
    }
}