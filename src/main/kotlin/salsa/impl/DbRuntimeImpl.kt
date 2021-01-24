package salsa.impl

import salsa.*
import java.util.ArrayDeque
import kotlin.math.max

class DbRuntimeImpl : DbRuntime {
    override var revision: Long = 0
    private val executionStack = ArrayDeque<QueryFrame<*, *>>()
    var eventLogger: ((RuntimeEvent) -> Unit)? = null
    // TODO store top level as a field

    override fun bumpRevision() {
        revision++
        logEvent { BumpRevision(revision) }
    }

    override fun <P, R> addAsDependency(queryDb: QueryDb<P, R>, parameters: P) {
        getQueryFrame()?.invocs?.add(QueryInvocation(queryDb, parameters))
    }

    override fun <P, R> pushFrame(query: Query<P, R>, parameters: P) {
        logEvent { PushFrame(query.key) }
        executionStack.push(QueryFrame(query, parameters))
    }

    override fun popFrame() : Frame {
        val frame = executionStack.pop()
        val maxRevision = frame.maxRevision
        logEvent { PopFrame(frame.query.key, maxRevision) }
        return frame
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

    override fun emitEvent(event: RuntimeEvent) {
        eventLogger?.invoke(event)
    }

    override fun hasLogger(): Boolean {
        return eventLogger != null
    }

    private fun getQueryFrame(): QueryFrame<*, *>? = executionStack.peekFirst()
}

private class QueryFrame<P, R>(val query: Query<P, R>, val parameters: P) : Frame {
    val invocs: MutableList<QueryInvocation<*, *>> = ArrayList()
    override val invocations: List<QueryInvocation<*, *>>
        get() = invocs
    override var maxRevision: Long = -1
}