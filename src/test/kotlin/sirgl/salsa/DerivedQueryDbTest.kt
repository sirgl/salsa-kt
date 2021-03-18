package sirgl.salsa

import kotlinx.coroutines.runBlocking
import org.junit.Test
import salsa.DerivedQueryKey
import salsa.tracing.*
import sirgl.salsa.utils.*
import kotlin.contracts.ExperimentalContracts
import kotlin.test.assertEquals

@ExperimentalContracts
class DerivedQueryDbTest {
    @Test
    fun testDerivedQueryExecute() {
        runBlocking {
            val context = defaultContext()
            context.queryRegistry.registerInputQuery(intIntKey)
            context.queryRegistry.registerDerivedQuery(Sum2Query.KEY, { Sum2Query(it, intIntKey) })
            val branch = context.branchRegistry.createEmptyBranch(defaultParams())
            branch.setInput(intIntKey, 1, 2)
            branch.setInput(intIntKey, 2, 4)

            val result = branch.executeQuery(Sum2Query.KEY, 1 to 2, "top level")
            assertEquals(6, result)
        }
    }

    @Test
    fun testDerivedQueryRecomputeResultAfterInputsChange() {
        runBlocking {
            val context = defaultContext()
            context.queryRegistry.registerInputQuery(intIntKey)
            context.queryRegistry.registerDerivedQuery(Sum2Query.KEY, { Sum2Query(it, intIntKey) })
            val branch = context.branchRegistry.createEmptyBranch(defaultParams())
            branch.setInput(intIntKey, 1, 2)
            branch.setInput(intIntKey, 2, 4)

            assertEquals(6, branch.executeQuery(Sum2Query.KEY, 1 to 2, "top level"))
            branch.setInput(intIntKey, 2, 6)
            assertEquals(8, branch.executeQuery(Sum2Query.KEY, 1 to 2, "top level"))
        }
    }

    @Test
    fun testDerivedQueryBasicEvents() {
        runBlocking {
            val context = defaultContext()
            val tracer = CollectingTracer()
            context.tracer = tracer
            context.queryRegistry.registerInputQuery(intIntKey)
            context.queryRegistry.registerDerivedQuery(Sum2Query.KEY, { Sum2Query(it, intIntKey) })
            val branch = context.branchRegistry.createEmptyBranch(defaultParams())
            branch.setInput(intIntKey, 1, 2)
            branch.setInput(intIntKey, 2, 4)
            tracer.removeAll()

            assertEquals(6, branch.executeQuery(Sum2Query.KEY, 1 to 2, "top level"))
            val events = tracer.removeAll().first().second
            assertEquals(listOf(
                TopLevelQueryStarted,
                QueryStarted(Sum2Query.KEY, 1 to 2),
                GetInputEvent(intIntKey, 1),
                GetInputEvent(intIntKey, 2),
                QueryFinished(Sum2Query.KEY, 1 to 2),
                TopLevelQueryFinished
            ), events)
        }
    }

    @Test
    fun testDerivedQuery2ndTime() {
        runBlocking {
            val context = defaultContext()
            val tracer = CollectingTracer()
            context.tracer = tracer
            context.queryRegistry.registerInputQuery(intIntKey)
            context.queryRegistry.registerDerivedQuery(Sum2Query.KEY, { Sum2Query(it, intIntKey) })
            val branch = context.branchRegistry.createEmptyBranch(defaultParams())
            branch.setInput(intIntKey, 1, 2)
            branch.setInput(intIntKey, 2, 4)
            assertEquals(6, branch.executeQuery(Sum2Query.KEY, 1 to 2, "top level"))
            tracer.removeAll()

            assertEquals(6, branch.executeQuery(Sum2Query.KEY, 1 to 2, "top level"))
            val events = tracer.removeAll().first().second
            assertEquals(listOf(
                TopLevelQueryStarted,
                QueryReused(QueryReuseType.SameRevision, Sum2Query.KEY, 1 to 2, 6),
                TopLevelQueryFinished
            ), events)
        }
    }

    @Test
    fun testDerivedQueryResultChangesAfterInputChange() {
        runBlocking {
            val context = defaultContext()
            val tracer = CollectingTracer()
            context.tracer = tracer
            context.queryRegistry.registerInputQuery(intIntKey)
            context.queryRegistry.registerDerivedQuery(Sum2Query.KEY, { Sum2Query(it, intIntKey) })
            val branch = context.branchRegistry.createEmptyBranch(defaultParams())
            branch.setInput(intIntKey, 1, 2)
            branch.setInput(intIntKey, 2, 4)
            assertEquals(6, branch.executeQuery(Sum2Query.KEY, 1 to 2, "top level"))
            branch.setInput(intIntKey, 1, 4)
            tracer.removeAll()

            assertEquals(8, branch.executeQuery(Sum2Query.KEY, 1 to 2, "top level"))
            val events = tracer.removeAll().first().second
            assertEquals(listOf(
                TopLevelQueryStarted,
                QueryStarted(Sum2Query.KEY, 1 to 2),
                GetInputEvent(intIntKey, 1),
                GetInputEvent(intIntKey, 2),
                QueryFinished(Sum2Query.KEY, 1 to 2),
                TopLevelQueryFinished
            ), events)
        }
    }

    @Test
    fun testDerivedQueryReusesResult() {
        runBlocking {
            val context = defaultContext()
            val traces = ArrayList<String>()
            context.tracer = EventHierarchyBuildingTracer(StringTraceBuilder { traces.add(it) })
            context.queryRegistry.registerInputQuery(intIntKey)
            context.queryRegistry.registerDerivedQuery(Sum2Query.KEY, { Sum2Query(it, intIntKey) })
            context.queryRegistry.registerDerivedQuery(Sum3Query.KEY, { Sum3Query(it, intIntKey) })
            val branch = context.branchRegistry.createEmptyBranch(defaultParams())
            branch.setInput(intIntKey, 1, 2)
            branch.setInput(intIntKey, 2, 4)
            branch.setInput(intIntKey, 3, 6)
            assertEquals(12, branch.executeQuery(Sum3Query.KEY, listOf(1, 2, 3), "top level"))
            branch.setInput(intIntKey, 3, 4)
            traces.clear()

            assertEquals(10, branch.executeQuery(Sum3Query.KEY, listOf(1, 2, 3), "top level"))
            val events = traces.single()
            assertEquals("""
                #5(top level) TopLevelQueryStarted
                  QueryStarted(DerivedQueryKey(test.sum.3), [1, 2, 3])
                    QueryReused(DependenciesNotChanged, DerivedQueryKey(test.sum.2), (1, 2) -> 6)
                    GetInputEvent(InputQueryKey(test.int.int), 3)
                  QueryFinished(DerivedQueryKey(test.sum.3), [1, 2, 3])
                TopLevelQueryFinished
                
            """.trimIndent(), events)
        }
    }

    @Test
    fun testDerivedConcurrentQueriesUpdated() {
        runBlocking {
            val context = defaultContext()
            val traces = ArrayList<String>()
            context.tracer = DispatchingTracer(EventHierarchyBuildingTracer(StringTraceBuilder { traces.add(it) }), EventHierarchyBuildingTracer(StdoutTraceBuilder))
            context.queryRegistry.registerInputQuery(intIntKey)
            context.queryRegistry.registerDerivedQuery(Sum2Query.KEY, { Sum2Query(it, intIntKey) })
            context.queryRegistry.registerDerivedQuery(Sum3Query.KEY, { Sum3Query(it, intIntKey) })
            val sum32 = DerivedQueryKey<List<Int>, Int>("sum3.2") // second variant of query to have 2 storages
            context.queryRegistry.registerDerivedQuery(sum32, { Sum3Query(it, intIntKey) })
            val branch = context.branchRegistry.createEmptyBranch(defaultParams())
            branch.setInput(intIntKey, 1, 2)
            branch.setInput(intIntKey, 2, 4)
            branch.setInput(intIntKey, 3, 6)
            assertEquals(12, branch.executeQuery(Sum3Query.KEY, listOf(1, 2, 3), "base"))
            assertEquals(12, branch.executeQuery(Sum3Query.KEY, listOf(1, 2, 3), "base"))
            assertEquals(12, branch.executeQuery(sum32, listOf(1, 2, 3), "concurrent"))
            branch.setInput(intIntKey, 1, 4)
            assertEquals(14, branch.executeQuery(Sum3Query.KEY, listOf(1, 2, 3), "base"))
            traces.clear()

            assertEquals(14, branch.executeQuery(sum32, listOf(1, 2, 3), "concurrent"))
            val events = traces.single()
            assertEquals("""
                #8(concurrent) TopLevelQueryStarted
                  QueryStarted(DerivedQueryKey(test.sum.3), [1, 2, 3])
                    QueryReused(SameRevision, DerivedQueryKey(test.sum.2), (1, 2) -> 8)
                    GetInputEvent(InputQueryKey(test.int.int), 3)
                  QueryFinished(DerivedQueryKey(test.sum.3), [1, 2, 3])
                TopLevelQueryFinished
                
            """.trimIndent(), events)
        }
    }

    @Test
    fun testDerivedQueryIsNotChanged() {
        runBlocking {
            val context = defaultContext()
            val traces = ArrayList<String>()
            context.tracer = EventHierarchyBuildingTracer(StringTraceBuilder { traces.add(it) })
            context.queryRegistry.registerInputQuery(intIntKey)
            context.queryRegistry.registerDerivedQuery(Sum2Query.KEY, { Sum2Query(it, intIntKey) })
            context.queryRegistry.registerDerivedQuery(Sum3Query.KEY, { Sum3Query(it, intIntKey) })
            val branch = context.branchRegistry.createEmptyBranch(defaultParams())
            branch.setInput(intIntKey, 1, 2)
            branch.setInput(intIntKey, 2, 4)
            branch.setInput(intIntKey, 3, 6)
            assertEquals(12, branch.executeQuery(Sum3Query.KEY, listOf(1, 2, 3), "top level"))
            branch.setInput(intIntKey, 1, 4)
            branch.setInput(intIntKey, 2, 2)
            traces.clear()

            assertEquals(12, branch.executeQuery(Sum3Query.KEY, listOf(1, 2, 3), "top level"))
            val events = traces.single()
            assertEquals("""
                #6(top level) TopLevelQueryStarted
                  QueryStarted(DerivedQueryKey(test.sum.3), [1, 2, 3])
                    QueryStarted(DerivedQueryKey(test.sum.2), (1, 2))
                      GetInputEvent(InputQueryKey(test.int.int), 1)
                      GetInputEvent(InputQueryKey(test.int.int), 2)
                    QueryFinished(DerivedQueryKey(test.sum.2), (1, 2))
                    GetInputEvent(InputQueryKey(test.int.int), 3)
                  QueryFinished(DerivedQueryKey(test.sum.3), [1, 2, 3])
                TopLevelQueryFinished
                
            """.trimIndent(), events)
        }
    }


    @Test
    fun testDerivedQueryIsNotChangedConcurrent() {
        runBlocking {
            val context = defaultContext()
            val traces = ArrayList<String>()
            context.tracer = EventHierarchyBuildingTracer(StringTraceBuilder { traces.add(it) })
            context.queryRegistry.registerInputQuery(intIntKey)
            context.queryRegistry.registerDerivedQuery(Sum2Query.KEY, { Sum2Query(it, intIntKey) })
            context.queryRegistry.registerDerivedQuery(Sum3Query.KEY, { Sum3Query(it, intIntKey) })
            val sum32 = DerivedQueryKey<List<Int>, Int>("sum3.2") // second variant of query to have 2 storages
            context.queryRegistry.registerDerivedQuery(sum32, { Sum3Query(it, intIntKey) })
            val branch = context.branchRegistry.createEmptyBranch(defaultParams())
            branch.setInput(intIntKey, 1, 2)
            branch.setInput(intIntKey, 2, 4)
            branch.setInput(intIntKey, 3, 6)
            assertEquals(12, branch.executeQuery(Sum3Query.KEY, listOf(1, 2, 3), "base"))
            assertEquals(12, branch.executeQuery(sum32, listOf(1, 2, 3), "other"))
            branch.setInput(intIntKey, 1, 4)
            branch.setInput(intIntKey, 2, 2)
            assertEquals(12, branch.executeQuery(Sum3Query.KEY, listOf(1, 2, 3), "base"))
            traces.clear()

            assertEquals(12, branch.executeQuery(sum32, listOf(1, 2, 3), "other"))
            val events = traces.single()
            assertEquals("""
                #8(other) TopLevelQueryStarted
                  QueryReused(DependenciesNotChanged, DerivedQueryKey(test.sum.3), [1, 2, 3] -> 12)
                TopLevelQueryFinished
                
            """.trimIndent(), events)
        }
    }
}