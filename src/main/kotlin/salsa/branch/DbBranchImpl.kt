package salsa.branch

import salsa.*
import salsa.context.DbContext
import salsa.frame.DbFrameImpl
import salsa.tracing.InputsUpdate
import salsa.tracing.TopLevelQueryFinished
import salsa.tracing.TopLevelQueryStarted
import salsa.tracing.TraceToken
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

class DbBranchImpl(
    val lock: ReentrantReadWriteLock,
    override val context: DbContext,
    branchParams: BranchParams,
    private val name: String? = null,
    baseRevision: DbRevision,
    private val parent: DbBranch? = null
) : DbBranch {

    // TODO reconsider this when there can be non transient forks
    override val queryDbProvider: QueryDbProvider = if (parent == null) {
        BranchQueryDbProvider(context.queryRegistry, BranchTraits(branchParams, false), this)
    } else {
        TransientDbProvider(context.queryRegistry, lock, parent, this)
    }

    private val forkedChildren = ArrayList<DbBranch>()

    // TODO use stamped lock here
    // TODO frozen must be under lock (it may be unfrozen after all branches are killed)
    @Volatile
    private var state = BranchState.Normal
    @Volatile
    private var cancelled = false


    override fun cancel() {
        // TODO log it somehow
        cancelled = true
    }

    override fun delete() {
        cancel()
        lock.write {
            if (forkedChildren.isNotEmpty()) {
                throw error("Branch deleted before its children")
            }
            // TODO log it somehow
            state = BranchState.Deleted
            if (parent != null) {
                parent as DbBranchImpl
                parent.notifyChildForkDeleted(this)
            }
        }
    }

    private fun notifyChildForkDeleted(child: DbBranch) {
        lock.write { // TODO probably no sense to do in under write action as it shouldn't affect main branch
            forkedChildren.remove(child)
            if (forkedChildren.isEmpty()) {
                state = BranchState.Normal
            }
        }
    }

    override fun isCancelled(): Boolean {
        return cancelled
    }

    override fun isDeleted(): Boolean {
        return state == BranchState.Deleted
    }

    override fun forkTransientAndFreeze(branchParams: BranchParams): DbBranch {
        state = BranchState.Frozen
        return DbBranchImpl(
            lock,
            context,
            branchParams,
            branchParams.name,
            revision,
            parent = this
        )
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
            val db = queryDbProvider.findDb(key)

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
        cancelled = true
        lock.write {
            if (state == BranchState.Frozen) {
                throw BranchFrozenException()
            } else if (state == BranchState.Deleted) {
                throw BranchDeletedException()
            }
            cancelled = false // TODO probably here I should atomically try to change value to false (otherwise some unfortunate query may have to wait because readers won't stop)
            for (change in diff) {
                change.applyChange(queryDbProvider)
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
    Frozen,
    Deleted,
}