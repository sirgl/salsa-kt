package salsa.impl

import salsa.*
import salsa.trace.EventLogger
import java.util.ArrayDeque
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.math.max

class DbRuntimeImpl : DbRuntime {
    override var revision: Long = 0
    private val executionStack = ArrayDeque<QueryFrame<*, *>>()
    var eventLogger: EventLogger? = null
    // TODO store top level as a field
    private val lock: ReentrantReadWriteLock = ReentrantReadWriteLock()
    @Volatile
    private var isWaitingWrite = false

    override fun bumpRevision() {
        revision++
        logEvent { BumpRevision(revision) }
    }

    override fun <P, R> pushFrame(query: Query<P, R>, parameters: P): Frame {
        logEvent { PushFrame(query.key) }
        val frame = QueryFrame(this, query, parameters)
        executionStack.push(frame)
        return frame
    }

    override fun popFrame() : Frame {
        val frame = executionStack.pop()
        val maxRevision = frame.maxRevision
        logEvent { PopFrame(frame.query.key, maxRevision) }
        return frame
    }

    override fun <P, R> willHaveCycleAfterAdding(query: Query<P, R>, parameters: P): Boolean {
        return executionStack.any { it.query == query && it.parameters == parameters }
    }

    override fun tryUpdateMaxChangedRevision(revision: Long) {
        val queryFrame = getQueryFrame() ?: return
        queryFrame.maxRevision = max(queryFrame.maxRevision, revision)
    }

    override fun gc() {
        TODO("Not yet implemented")
    }

    override fun forkTransient(): DbRuntime {
        TODO("Not yet implemented")
    }

    // TODO add here also current frame - now it is unclear to which thread it belongs
    override fun emitEvent(event: RuntimeEvent) {
        eventLogger?.logEvent(event)
    }

    override fun hasLogger(): Boolean {
        return eventLogger != null
    }

    override fun <R> withReadLock(action: () -> R): R {
        return lock.read(action)
    }

    override fun <R> withWriteLock(action: () -> R): R {
        isWaitingWrite = true
        return lock.write {
            isWaitingWrite = false
            action()
        }
    }

    override fun isWaitingForWrite(): Boolean {
        return isWaitingWrite
    }

    override fun createTopLevelFrame(name: String?): Frame {
        return object : Frame {
            override val invocations: List<QueryInvocation<*, *>>
                get() = emptyList()
            override val maxRevision: Long
                get() = -1

            override fun <P, R> createChildFrame(query: Query<P, R>, parameters: P): Frame {
                return QueryFrame(this@DbRuntimeImpl, query, parameters)
            }

            override fun tryUpdateMaxChangedRevision(revision: Long) {}

            override fun <P, R> addAsDependency(queryDb: QueryDb<P, R>, parameters: P) {}
            override val name: String? = name
        }
    }

    private fun getQueryFrame(): QueryFrame<*, *>? = executionStack.peekFirst()
}