package salsa.context

import salsa.DbBranchRegistry
import salsa.DbBranchRegistryImpl
import salsa.tracing.QueryTracer
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.contracts.ExperimentalContracts

// TODO context contains too much API!!! users outside shouldn't bother about most of the things!
// TODO this thing should be closable! (db can be inside!)
interface DbContext {
    val queryRegistry: QueryRegistry

    val branchRegistry: DbBranchRegistry

    val tracer: QueryTracer?

    // TODO here should also be some thing that manages branches (e.g. that can enumerate them)
    //  also this thing should know about the location of caches of these branches

    fun close()

    fun nextTraceTokenId() : Long
}

// TODO this thing should be closable!
interface DbBranchStorage {
    // TODO which information is required to restore information about branch?

    // TODO does branch itself has to be always in memory?
}

@ExperimentalContracts
class DbContextImpl(branchStorage: DbBranchStorage) : DbContext {
    private val lock = ReentrantReadWriteLock()
    private val traceTokenIdCounter = AtomicLong()

    override val queryRegistry: QueryRegistry = QueryRegistryImpl(lock)

    override val branchRegistry: DbBranchRegistry by lazy { DbBranchRegistryImpl(lock, this) }

    override var tracer: QueryTracer? = null

    override fun close() {
        for (branch in branchRegistry.getActiveBranches()) {
            branch.cancel()
        }
    }

    override fun nextTraceTokenId(): Long {
        return traceTokenIdCounter.getAndIncrement()
    }
}