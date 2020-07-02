package salsa.impl

import salsa.DbRuntime
import salsa.Frame
import salsa.Query
import salsa.QueryDb
import salsa.QueryInvocation
import java.util.ArrayDeque
import kotlin.math.max

class DbRuntimeImpl : DbRuntime {
    override var revision: Long = 0
    private val executionStack = ArrayDeque<QueryFrame<*, *>>()
    // TODO store top level as a field

    override fun bumpRevision() {
        revision++
    }

    override fun <P, R> addAsDependency(queryDb: QueryDb<P, R>, parameters: P) {
        getQueryFrame()?.invocs?.add(QueryInvocation(queryDb, parameters))
    }

    override fun <P, R> pushFrame(query: Query<P, R>, parameters: P) {
        executionStack.push(QueryFrame(query, parameters))
    }

    override fun popFrame() : Frame {
        return executionStack.pop()
    }

    override fun tryUpdateMaxChangedRevision(revision: Long) {
        val queryFrame = getQueryFrame() ?: return
        queryFrame.maxRevision = max(queryFrame.maxRevision, revision)
    }

    override fun gc() {
        TODO("Not yet implemented")
    }

    private fun getQueryFrame(): QueryFrame<*, *>? = executionStack.peekFirst()
}

private class QueryFrame<P, R>(val query: Query<P, R>, val parameters: P) : Frame {
    val invocs: MutableList<QueryInvocation<*, *>> = ArrayList()
    override val invocations: List<QueryInvocation<*, *>>
        get() = invocs
    override var maxRevision: Long = -1
}