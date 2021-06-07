package io.objectbox.ktx

import io.objectbox.TestEntity_
import io.objectbox.kotlin.inValues
import io.objectbox.kotlin.query
import io.objectbox.query.AbstractQueryTest
import org.junit.Assert.assertEquals
import org.junit.Test


class QueryBuilderKt : AbstractQueryTest() {

    /**
     * This is mostly to test the expected syntax works without errors or warnings.
     */
    @Test
    fun queryBlock_and_inValues() {
        putTestEntitiesScalars()
        val valuesLong = longArrayOf(3000)

        val resultJava = box.query().`in`(TestEntity_.simpleLong, valuesLong).build().use {
            it.findFirst()
        }
        val result = box.query {
            inValues(TestEntity_.simpleLong, valuesLong)
        }.use {
            it.findFirst()
        }
        assertEquals(resultJava!!.id, result!!.id)
    }
}