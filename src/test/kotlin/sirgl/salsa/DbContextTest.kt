package sirgl.salsa

import kotlinx.coroutines.runBlocking
import org.junit.Test
import salsa.*
import salsa.branch.AtomInputChange
import sirgl.salsa.utils.defaultContext
import sirgl.salsa.utils.defaultParams
import sirgl.salsa.utils.intStringKey
import kotlin.contracts.ExperimentalContracts
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalContracts
class DbContextTest {
    @Test
    fun testBranchCancelledAfterContextIsClosed() {
        val context = defaultContext()
        val branch = context.branchRegistry.createEmptyBranch(defaultParams())

        context.close()
        assertTrue(branch.isCancelled())
    }

    @Test
    fun testBranchRegistered() {
        val context = defaultContext()
        val branch = context.branchRegistry.createEmptyBranch(defaultParams())
        assertTrue(context.branchRegistry.getActiveBranches().toSet().contains(branch))
        assertEquals(0, branch.revision)
    }

    @Test
    fun testAddInputQuery() {
        val context = defaultContext()
        context.queryRegistry.registerInputQuery(intStringKey)
        val branch = context.branchRegistry.createEmptyBranch(defaultParams())
        branch.applyInputDiff(listOf(AtomInputChange(intStringKey, 10, "Hello", Durability.High)))
        assertEquals(1, branch.revision)
        val result = runBlocking {
            branch.executeQuery(intStringKey, 10, "top level")
        }
        assertEquals("Hello", result)
    }
}