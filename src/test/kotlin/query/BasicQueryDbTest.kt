package query

import salsa.BasicQuery
import salsa.QueryKey
import salsa.impl.BasicQueryDbImpl
import salsa.impl.DbRuntimeImpl
import org.junit.Test
import kotlin.test.assertEquals

class BaseQueryDbTest {
    @Test
    fun testBasic() {
        val q1 = BasicQuery<Int, String>(QueryKey("q1"))
        val runtime = DbRuntimeImpl()
        val q1Db = BasicQueryDbImpl(runtime, q1)
        assertEquals(runtime.revision, 0)
        q1Db.set(1, "1")
        q1Db.set(2, "2")
        assertEquals(runtime.revision, 2)
        assertEquals(q1Db.eval(1), "1")
        assertEquals(q1Db.eval(2), "2")
        q1Db.set(1, "new")
        assertEquals(runtime.revision, 3)
        assertEquals(q1Db.eval(1), "new")
    }

    fun testForPresentation() {
        val runtime = DbRuntimeImpl()
        val fileTextQuery = BasicQuery<Int, String>(QueryKey("file text"))
        val fileTextQueryDb = BasicQueryDbImpl(runtime, fileTextQuery)
        val file1 = 1
        val file2 = 2

        // revision R0
        fileTextQueryDb[file1] = "class A {}" // revision R0 + 1
        fileTextQueryDb[file1] = "class B {}" // revision R0 + 2
        fileTextQueryDb[file2] = "class Other {}" // revision R0 + 3
    }
}