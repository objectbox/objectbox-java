package io.objectbox

import app.cash.turbine.test
import io.objectbox.kotlin.flow
import io.objectbox.kotlin.query
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.ExperimentalTime


class FlowTest : AbstractObjectBoxTest() {

    @ExperimentalTime
    @ExperimentalCoroutinesApi
    @Test
    fun flow_box() {
        runBlocking {
            store.flow(TestEntity::class.java).test {
                assertEquals(TestEntity::class.java, expectItem())
                putTestEntities(1)
                // Note: expectItem suspends until event, so no need to wait on OBX publisher thread.
                assertEquals(TestEntity::class.java, expectItem())
                cancel() // expect no more events
            }
        }
    }

    @ExperimentalTime
    @ExperimentalCoroutinesApi
    @Test
    fun flow_query() {
        runBlocking {
            testEntityBox.query {}.flow().test {
                assertEquals(0, expectItem().size)
                putTestEntities(1)
                // Note: expectItem suspends until event, so no need to wait on OBX publisher thread.
                assertEquals(1, expectItem().size)
                cancel() // expect no more events
            }
        }
    }
}