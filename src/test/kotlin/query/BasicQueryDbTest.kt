package query

import salsa.impl.BaseQueryDbImpl
import salsa.impl.DbRuntimeImpl
import org.junit.Test
import salsa.*
import salsa.cache.InMemoryBaseCache
import java.util.ArrayList
import kotlin.test.assertEquals

class BaseQueryDbTest {
    @Test
    fun testBasic() {
        val q = BasicQuery<Int, String>(QueryKey("q1"))
        val runtime = DbRuntimeImpl()
        val q1Db = BaseQueryDbImpl(runtime, q, InMemoryBaseCache())
        assertEquals(runtime.revision, 0)
        q1Db[1] = "1"
        q1Db[2] = "2"
        assertEquals(runtime.revision, 2)
        val topLevel = runtime.topLevelFrame
        assertEquals(q1Db[topLevel, 1], "1")
        assertEquals(q1Db[topLevel, 2], "2")
        q1Db[1] = "new"
        assertEquals(runtime.revision, 3)
        assertEquals(q1Db[topLevel, 1], "new")
    }

    @Test
    fun testBasicEvents() {
        val q = BasicQuery<Int, String>(QueryKey("q1"))
        val runtime = DbRuntimeImpl()
        val qDb = BaseQueryDbImpl(runtime, q, InMemoryBaseCache())
        val events: MutableList<RuntimeEvent> = ArrayList()
        runtime.eventLogger = { event ->
            events.add(event)
        }
        qDb[1] = "asdas"
        val v = qDb[runtime.topLevelFrame, 1]
        assertEquals("asdas", v)
        assertEquals(listOf(
            BumpRevision(currentRevision=1),
            SetBase(parameters=1, value="asdas"),
            GetBase(parameters=1, value="asdas")
        ), events)
    }
}