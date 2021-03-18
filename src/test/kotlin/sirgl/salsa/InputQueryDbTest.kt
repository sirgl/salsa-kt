package sirgl.salsa

import kotlinx.coroutines.runBlocking
import org.junit.Test
import salsa.tracing.*
import sirgl.salsa.utils.*
import kotlin.contracts.ExperimentalContracts
import kotlin.test.assertEquals

@ExperimentalContracts
class InputQueryDbTest {
    @Test
    fun testInputSet() {
        runBlocking {
            val context = defaultContext()
            context.queryRegistry.registerInputQuery(intStringKey)
            val branch = context.branchRegistry.createEmptyBranch(defaultParams())
            assertEquals(0, branch.revision)
            branch.setInput(intStringKey, 1, "Hello")
            branch.setInput(intStringKey, 2, "Bar")
            assertEquals("Hello", branch.executeQuery(intStringKey, 1, "Top level 1"))
            assertEquals("Bar", branch.executeQuery(intStringKey, 2, "Top level 1"))
            assertEquals(2, branch.revision)
            branch.setInput(intStringKey, 1, "Bye")
            assertEquals("Bye", branch.executeQuery(intStringKey, 1, "Top level 1"))
            assertEquals(3, branch.revision)
        }
    }

    @Test
    fun testEventsWhileChangingInput() {
        runBlocking {
            val context = defaultContext()
            val tracer = CollectingTracer()
            context.tracer = tracer
            context.queryRegistry.registerInputQuery(intStringKey)
            val branch = context.branchRegistry.createEmptyBranch(defaultParams())
            branch.setInput(intStringKey, 1, "Hello")

            val updateEvents = tracer.removeAll().first().second
            assertEquals(listOf(InputsUpdate(listOf(intStringKey to 1))), updateEvents)

            val result = branch.executeQuery(intStringKey, 1, "Top level")
            assertEquals(result, "Hello")
            val queryEvents = tracer.removeAll().first().second
            assertEquals(listOf(
                TopLevelQueryStarted,
                GetInputEvent(intStringKey, 1),
                TopLevelQueryFinished
            ), queryEvents)
        }
    }
}