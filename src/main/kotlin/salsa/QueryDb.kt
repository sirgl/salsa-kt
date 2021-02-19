package salsa

import kotlin.math.max

/**
 * storage of info associated with a given query
 */
interface QueryDb<P, R> {
    val query: Query<P, R>

    /**
     * Computes or returns previously recorded value.
     *
     * Implementations:
     * Should ensure that there is no cycle.
     * Before returning the result it must set in runtime maximal changedAt revision that was touched during the computation.
     */
    operator fun get(parent: Frame, parameters: P) : R
    fun getRevisionOfLastChange(parameters: P) : Long
}

class QueryFrame<P, R>(val runtime: DbRuntime, val query: Query<P, R>, val parameters: P) : Frame {
    val _invocations: MutableList<QueryInvocation<*, *>> = ArrayList()
    override val invocations: List<QueryInvocation<*, *>>
        get() = _invocations
    override var maxRevision: Long = -1

    override fun <P1, R1> createChildFrame(query: Query<P1, R1>, parameters: P1): Frame {
        return QueryFrame(runtime, query, parameters)
    }

    override fun tryUpdateMaxChangedRevision(revision: Long) {
        maxRevision = max(revision, maxRevision)
    }

    override fun <P, R> addAsDependency(queryDb: QueryDb<P, R>, parameters: P) {
        _invocations.add(QueryInvocation(queryDb, parameters))
    }
}

interface QueryDbProvider {
    fun <P, R> getQueryDb(key: QueryKey<P, R>) : QueryDb<P, R>
}

interface BaseQueryDb<P, R> : QueryDb<P, R> {
    // TODO can't set it during executing another query (or we are inside a query)
    operator fun set(params: P, value: R)
}

class CycleException(val query: Query<*, *>, val parameters: Any?) : Exception()