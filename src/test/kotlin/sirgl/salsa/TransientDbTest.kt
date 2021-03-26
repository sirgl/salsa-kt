package sirgl.salsa

import kotlinx.coroutines.runBlocking
import org.junit.Test
import salsa.branch.BranchFrozenException
import salsa.branch.BranchParams
import sirgl.salsa.utils.*
import kotlin.contracts.ExperimentalContracts
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExperimentalContracts
class TransientDbTest {
    @Test(expected = BranchFrozenException::class)
    fun testAfterTransientForkCantUpdateMainBranch() {
        runBlocking {
            val context = defaultContext()
            context.queryRegistry.registerInputQuery(intStringKey)
            val branch = context.branchRegistry.createEmptyBranch(defaultParams())
            assertEquals(0, branch.revision)
            branch.setInput(intStringKey, 1, "main")
            val params = BranchParams(isDurable = false, isLinear = false, name = "my transient branch")
            val forkedBranch = branch.forkTransientAndFreeze(params)
            forkedBranch.setInput(intStringKey, 1, "forked")
            branch.setInput(intStringKey, 1, "new forked")
        }
    }

    @Test
    fun testAfterForkedBranchDeletedMainBranchUnfreezes() {
        runBlocking {
            val context = defaultContext()
            context.queryRegistry.registerInputQuery(intStringKey)
            val branch = context.branchRegistry.createEmptyBranch(defaultParams())
            assertEquals(0, branch.revision)
            branch.setInput(intStringKey, 1, "main")
            val params = BranchParams(isDurable = false, isLinear = false, name = "my transient branch")
            val forkedBranch1 = branch.forkTransientAndFreeze(params)
            val forkedBranch2 = branch.forkTransientAndFreeze(params)
            forkedBranch1.setInput(intStringKey, 1, "forked1")
            forkedBranch1.setInput(intStringKey, 1, "forked2")
            assertTrue(branch.isFrozen())
            forkedBranch1.delete()
            forkedBranch2.delete()
            assertTrue(!branch.isFrozen())
            branch.setInput(intStringKey, 1, "after")
        }
    }

    @Test
    fun testTransientForkInputsChangesButMainBranchIsStillQueriable() {
        runBlocking {
            val context = defaultContext()
            context.queryRegistry.registerInputQuery(intStringKey)
            val branch = context.branchRegistry.createEmptyBranch(defaultParams())
            assertEquals(0, branch.revision)
            branch.setInput(intStringKey, 1, "Hello")
            branch.setInput(intStringKey, 2, "Bar")
            val params = BranchParams(isDurable = false, isLinear = false, name = "my transient branch")
            val forkedBranch = branch.forkTransientAndFreeze(params)
            assertTrue(branch.isFrozen())
            forkedBranch.setInput(intStringKey, 1, "Bye")

            assertEquals(2, branch.revision)
            assertEquals(3, forkedBranch.revision)
            assertEquals("Bye", forkedBranch.executeQuery(intStringKey, 1, "Top level"))
            assertEquals("Bar", forkedBranch.executeQuery(intStringKey, 2, "Top level"))
            assertEquals("Hello", branch.executeQuery(intStringKey, 1, "Top level"))
        }
    }

    @Test
    fun testTransientForkDerivedValues() {
        runBlocking {
            val context = defaultContext()
            context.queryRegistry.registerInputQuery(intIntKey)
            context.queryRegistry.registerDerivedQuery(Sum2Query.KEY, { Sum2Query(it, intIntKey) })
            val branch = context.branchRegistry.createEmptyBranch(defaultParams())
            assertEquals(0, branch.revision)
            branch.setInput(intIntKey, 1, 10)
            branch.setInput(intIntKey, 2, 20)
            val params = BranchParams(isDurable = false, isLinear = false, name = "my transient branch")
            val forkedBranch = branch.forkTransientAndFreeze(params)
            assertTrue(branch.isFrozen())
            forkedBranch.setInput(intIntKey, 1, 30)

            assertEquals(30, branch.executeQuery(Sum2Query.KEY, 1 to 2, "Top level"))
            assertEquals(50, forkedBranch.executeQuery(Sum2Query.KEY, 1 to 2, "Top level"))
            assertEquals(30, branch.executeQuery(Sum2Query.KEY, 1 to 2, "Top level"))
        }
    }
}