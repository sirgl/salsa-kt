package query

import salsa.BasicQuery
import salsa.DerivedQuery
import salsa.QueryDb
import salsa.QueryKey
import salsa.impl.BaseQueryDbImpl
import salsa.impl.DbRuntimeImpl
import salsa.impl.DependencyTrackingDerivedQueryDbImpl
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class DerivedQueryDbTest {
    @Test
    fun testQueryChangesAfterInputsChanged() {
        val runtime = DbRuntimeImpl()
        val q1 = BasicQuery<Int, String>(QueryKey("q1"))
        val q1Db = BaseQueryDbImpl(runtime, q1)
        val q2 = BasicQuery<Int, String>(QueryKey("q2"))
        val q2Db = BaseQueryDbImpl(runtime, q2)
        q1Db[1] = "1"
        q2Db[1] = "2"
        val concat: QueryDb<Pair<Int, Int>, String> = DependencyTrackingDerivedQueryDbImpl(runtime, ConcatQuery(q1Db, q2Db))
        assertEquals("12", concat[1 to 1])
        q1Db[1] = "3"
        assertEquals("32", concat[1 to 1])
    }

    @Test
    fun testDerivedNotUpdatedIfResultTheSame() {
        val runtime = DbRuntimeImpl()
        val q1 = BasicQuery<Int, Int>(QueryKey("q1"))
        val q1Db = BaseQueryDbImpl(runtime, q1)
        val q2 = BasicQuery<Int, Int>(QueryKey("q2"))
        val q2Db = BaseQueryDbImpl(runtime, q2)
        q1Db[1] = 10
        q2Db[1] = 20
        val sum: QueryDb<Pair<Int, Int>, CountedInt> = DependencyTrackingDerivedQueryDbImpl(runtime, SumQuery(q1Db, q2Db))
        val sum1 = sum[1 to 1]
        q1Db[1] = 5
        q2Db[1] = 25
        val sum2 = sum[1 to 1]
        assertSame(sum1, sum2)
    }
}

class ConcatQuery(private val q1: QueryDb<Int, String>, private val q2: QueryDb<Int, String>) :
    DerivedQuery<Pair<Int, Int>, String> {
    override val key: QueryKey<Pair<Int, Int>, String>
        get() = QueryKey("concat")

    override fun doQuery(params: Pair<Int, Int>): String {
        val value1 = q1[params.first]
        val value2 = q2[params.first]
        return value1 + value2
    }
}

class CountedInt(val value: Int, val count: Int = counter++) {
    companion object {
        var counter = 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CountedInt) return false

        if (value != other.value) return false

        return true
    }

    override fun hashCode(): Int = value
}

class SumQuery(val q1: QueryDb<Int, Int>, val q2: QueryDb<Int, Int>) :
    DerivedQuery<Pair<Int, Int>, CountedInt> {
    override val key: QueryKey<Pair<Int, Int>, CountedInt> = QueryKey("sum")

    override fun doQuery(params: Pair<Int, Int>): CountedInt {
        return CountedInt(q1[params.first] + q2[params.second])
    }
}