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
        putTestEntities(1)

        runBlocking {
            store.flow(TestEntity::class.java).test {
                assertEquals(TestEntity::class.java, expectItem())
                putTestEntities(1)
                assertEquals(TestEntity::class.java, expectItem())
                cancel() // expect no more events
            }
        }
    }

    @ExperimentalTime
    @ExperimentalCoroutinesApi
    @Test
    fun flow_query() {
        putTestEntities(1)

        runBlocking {
            testEntityBox.query {}.flow().test {
                assertEquals(1, expectItem().size)
                putTestEntities(1)
                assertEquals(2, expectItem().size)
                cancel() // expect no more events
            }
        }
    }
}