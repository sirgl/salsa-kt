package query

import salsa.impl.BaseQueryDbImpl
import salsa.impl.DbRuntimeImpl
import salsa.impl.DependencyTrackingDerivedQueryDbImpl
import org.junit.Test
import salsa.*
import java.util.ArrayList
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
        val concat: QueryDb<Pair<Int, Int>, String> = DependencyTrackingDerivedQueryDbImpl(runtime, Concat2Query(q1Db))
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

    @Test
    fun testEventsNotChanged1LevelDerived() {
        val runtime = DbRuntimeImpl()

        val q = BasicQuery<Int, String>(QueryKey("q"))
        val qDb = BaseQueryDbImpl(runtime, q)
        qDb[1] = "foo"
        qDb[2] = "bar"
        val concatQuery= DependencyTrackingDerivedQueryDbImpl(runtime, Concat2Query(qDb))
        assertEquals(concatQuery[1 to 2], "foobar")
        qDb[1] = "fo"
        qDb[2] = "obar"
        val events: MutableList<RuntimeEvent> = ArrayList()
        runtime.eventLogger = { event ->
            events.add(event)
        }
        assertEquals(concatQuery[1 to 2], "foobar")
        assertEquals(listOf(
            PushFrame(key = Concat2Query.key),
            GetBase(parameters = 1, value = "fo"),
            GetBase(parameters = 2, value = "obar"),
            PopFrame(Concat2Query.key, 3),
            DerivedMemoNotChanged(key = Concat2Query.key)
        ), events)
    }

    @Test
    fun testEventsChanged1LevelDerived() {
        val runtime = DbRuntimeImpl()

        val q = BasicQuery<Int, String>(QueryKey("q"))
        val qDb = BaseQueryDbImpl(runtime, q)
        qDb[1] = "foo"
        qDb[2] = "bar"
        val concatQuery= DependencyTrackingDerivedQueryDbImpl(runtime, Concat2Query(qDb))
        assertEquals(concatQuery[1 to 2], "foobar")
        qDb[1] = "baz"
        val events: MutableList<RuntimeEvent> = ArrayList()
        runtime.eventLogger = { event ->
            events.add(event)
        }
        assertEquals(concatQuery[1 to 2], "bazbar")
        assertEquals(listOf(
            PushFrame(key = Concat2Query.key),
            GetBase(parameters = 1, value = "baz"),
            GetBase(parameters = 2, value = "bar"),
            PopFrame(key = Concat2Query.key, maxRevision = 2),
            DerivedMemoUpdated(key = Concat2Query.key, value = "bazbar")
        ), events)
    }

    @Test
    fun testEventsChanged2LevelDerived() {
        val runtime = DbRuntimeImpl()

        val q = BasicQuery<Int, String>(QueryKey("q"))
        val qDb = BaseQueryDbImpl(runtime, q)
        qDb[1] = "foo"
        qDb[2] = "bar"
        qDb[3] = "baz"
        val concatQuery= DependencyTrackingDerivedQueryDbImpl(runtime, Concat2Query(qDb))
        assertEquals(concatQuery[1 to 2], "foobar")
        val (concat3Key, concat3Query: DerivedQuery<List<Int>, String>) = query<List<Int>, String>("concatTriple") {
            val (k1, k2, k3) = it
            val concat2 = concatQuery[k1 to k2]
            val third = qDb[k3]
            concat2 + third
        }
        val concat3QDb = DependencyTrackingDerivedQueryDbImpl(runtime, concat3Query)
        assertEquals(concat3QDb[listOf(1, 2, 3)], "foobarbaz")
        qDb[1] = "fo"
        qDb[2] = "obar"
        val events: MutableList<RuntimeEvent> = ArrayList()
        runtime.eventLogger = { event ->
            events.add(event)
        }
        assertEquals(concat3QDb[listOf(1, 2, 3)], "foobarbaz")
        assertEquals(listOf(
            PushFrame(key = concat3Key),
            PushFrame(key = Concat2Query.key),
            GetBase(parameters = 1, value = "fo"),
            GetBase(parameters = 2, value = "obar"),
            PopFrame(key = Concat2Query.key, maxRevision = 4),
            DerivedMemoNotChanged(key = Concat2Query.key),
            GetBase(parameters = 3, value = "baz"),
            PopFrame(key = concat3Key, maxRevision = 5),
            DerivedMemoNotChanged(key = concat3Key)
        ), events)
    }
}

fun <P: Any, R: Any> query(key: String, q: (P) -> R) : Pair<QueryKey<P, R>, DerivedQuery<P, R>> {
    val queryKey = QueryKey<P, R>(key)
    return queryKey to object : DerivedQuery<P, R> {
        override val key: QueryKey<P, R>
            get() = queryKey

        override fun doQuery(params: P): R {
            return q(params)
        }
    }
}

class Concat2Query(private val q: QueryDb<Int, String>) :
    DerivedQuery<Pair<Int, Int>, String> {
    companion object {
        val key: QueryKey<Pair<Int, Int>, String> = QueryKey("concat")
    }

    override val key: QueryKey<Pair<Int, Int>, String>
        get() = QueryKey("concat")

    override fun doQuery(params: Pair<Int, Int>): String {
        val value1 = q[params.first]
        val value2 = q[params.second]
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