package salsa.branch

import salsa.*
import salsa.context.DbContext
import salsa.frame.DbFrameImpl
import salsa.tracing.*
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class DbBranchImpl(
    val lock: ReentrantReadWriteLock,
    override val context: DbContext,
    branchParams: BranchParams,
    val name: String? = null,
    baseRevision: DbRevision
) : DbBranch {
    private val dbProvider: QueryDbProvider = BranchQueryDbProvider(context.queryRegistry, branchParams, this)

    // TODO use stamped lock here
    // TODO frozen must be under lock (it may be unfrozen after all branches are killed)
    @Volatile
    private var state = BranchState.Normal


    override fun cancel() {
        // TODO here we should log it somehow
        state = BranchState.Cancelled
    }

    override fun isCancelled(): Boolean {
        return state == BranchState.Cancelled
    }

    override fun freezeAndFork(branchParams: BranchParams): DbBranch {
        state = BranchState.Frozen
        // TODO make forked storage
        return DbBranchImpl(lock, context, branchParams, branchParams.name, revision)
    }

    override fun isFrozen(): Boolean {
        return state == BranchState.Frozen
    }

    override val canFork: Boolean
        get() = TODO("Not yet implemented")


    private val revisionAtomic = AtomicLong(baseRevision)

    override val revision: DbRevision
        get() = revisionAtomic.get()

    override fun fork(strategy: BranchParams): DbBranch {
        TODO("Not yet implemented")
    }

    override suspend fun <P, R> executeQuery(key: QueryKey<P, R>, params: P, name: String): R {
        lock.read {
            val db = dbProvider.findDb(key)

            val frame = DbFrameImpl(this, createTraceToken(name))
            frame.trace { TopLevelQueryStarted }
            try {
                // TODO shouldn't I use thread pool and some context + scope?
                return db.executeQuery(frame, params)
            } catch (e: Exception) { // TODO handle cycles, kill state
                e.printStackTrace() // TODO something better, e. g. log?
                throw e
            } finally {
                frame.trace { TopLevelQueryFinished }
            }
        }
    }

    private fun createTraceToken(name: String?) = TraceToken(context.nextTraceTokenId(), name)

    override fun applyInputDiff(diff: List<AtomInputChange<*, *>>, name: String?) {
        // TODO if frozen - throw exception
        // TODO cancel all input requests, prepare to do write action
        state = BranchState.Cancelled
        lock.write {
            for (change in diff) {
                change.applyChange(dbProvider)
            }
            revisionAtomic.incrementAndGet()
            state = BranchState.Normal
        }
        trace(createTraceToken(name)) { InputsUpdate(diff.map { it.key to it.params }) }
    }

    override fun toString(): String {
        return lock.read {
            buildString {
                append("Branch")
                // TODO add frozen and cancelled traits
                if (name != null) {
                    append(" ")
                    append(name)
                }
            }
        }
    }
}

private enum class BranchState {
    Normal,
    Cancelled,
    Frozen,
}