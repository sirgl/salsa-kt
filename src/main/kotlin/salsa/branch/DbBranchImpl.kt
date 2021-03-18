package salsa.branch

import salsa.*
import salsa.context.DbContext
import salsa.frame.DbFrameImpl
import salsa.tracing.*
import java.util.ArrayList
import java.util.concurrent.locks.ReentrantReadWriteLock
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
    private var isKilled = false

    @Volatile
    private var isFrozen = false // TODO it may be unfrozen after all branches


    override fun cancel() {
        isKilled = true
    }

    override fun isCancelled(): Boolean {
        return isKilled
    }

    override fun freezeAndFork(branchParams: BranchParams): DbBranch {
        isFrozen = true
        // TODO make forked storage
        return DbBranchImpl(lock, context, branchParams, branchParams.name, revision)
    }

    override val canFork: Boolean
        get() = TODO("Not yet implemented")

    override var revision: DbRevision = baseRevision
        private set

    override fun fork(strategy: BranchParams): DbBranch {
        TODO("Not yet implemented")
    }

    override suspend fun <P, R> executeQuery(key: QueryKey<P, R>, params: P, name: String): R {
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

    private fun createTraceToken(name: String?) = TraceToken(context.nextTraceTokenId(), name)

    override fun applyInputDiff(diff: List<AtomInputChange<*, *>>, name: String?) {
        // TODO cancel all input requests, prepare to do write action
        lock.write {
            for (change in diff) {
                change.applyChange(dbProvider)
            }
            revision++
        }
        trace(createTraceToken(name)) { InputsUpdate(diff.map { it.key to it.params }) }
    }

    override fun isFrozen(): Boolean {
        return isFrozen
    }

    override fun toString(): String {
        return buildString {
            append("Branch")
            // TODO add frozen and cancelled traits
            if (name != null) {
                append(" ")
                append(name)
            }
        }
    }
}
