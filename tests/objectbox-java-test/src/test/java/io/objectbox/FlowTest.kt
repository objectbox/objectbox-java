package io.objectbox

import app.cash.turbine.test
import io.objectbox.kotlin.flow
import io.objectbox.kotlin.query
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test


class FlowTest : AbstractObjectBoxTest() {

    @Test
    fun flow_box() {
        runBlocking {
            store.flow(TestEntity::class.java).test {
                assertEquals(TestEntity::class.java, awaitItem())
                putTestEntities(1)
                // Note: awaitItem suspends until event, so no need to wait on OBX publisher thread.
                assertEquals(TestEntity::class.java, awaitItem())
                cancel() // expect no more events
            }
        }
    }

    @Test
    fun flow_query() {
        runBlocking {
            testEntityBox.query {}.flow().test {
                assertEquals(0, awaitItem().size)
                putTestEntities(1)
                // Note: awaitItem suspends until event, so no need to wait on OBX publisher thread.
                assertEquals(1, awaitItem().size)
                cancel() // expect no more events
            }
        }
    }
}