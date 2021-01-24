package salsa


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
    operator fun get(parameters: P) : R
    fun changed(parameters: P) : Long
    fun forkTransient() : QueryDb<P, R>
}

interface QueryDbProvider {
    fun <P, R> getQueryDb(key: QueryKey<P, R>) : QueryDb<P, R>
}

interface BasicQueryDb<P, R> : QueryDb<P, R> {
    // TODO can't set it during executing another query (or we are inside a query)
    operator fun set(params: P, value: R)

    override fun forkTransient() : BasicQueryDb<P, R>
}