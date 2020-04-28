package io.objectbox.rx3

import io.objectbox.query.Query
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class QueryKtxTest {

    @Test
    fun flowableFromQuery() {
        val observable = Mockito.mock(Query::class.java).flowableOneByOne()
        assertNotNull(observable)
    }

    @Test
    fun observableFromQuery() {
        val observable = Mockito.mock(Query::class.java).observable()
        assertNotNull(observable)
    }

    @Test
    fun singleFromQuery() {
        val observable = Mockito.mock(Query::class.java).single()
        assertNotNull(observable)
    }
}