package salsa


/**
 * Runtime for the database, stores revision counter and [Frame] stack
 */
interface DbRuntime {
    /**
     * Revision of database (count of changes of inputs)
     */
    val revision: Long

    /**
     * Advance revision counter
     */
    fun bumpRevision()

    /**
     * Add dependency to the currently active query (or do nothing if it is entrance query)
     */
    fun <P, R> addAsDependency(queryDb: QueryDb<P, R>, parameters: P)

    /**
     * Add new frame to the stack and make it active
     */
    fun <P, R> pushFrame(query: Query<P, R>, parameters: P)

    /**
     * Remove previous frame from the stack and
     */
    fun popFrame() : Frame

    /**
     * Updates [Frame.maxRevision] if [revision] is greater
     */
    fun tryUpdateMaxChangedRevision(revision: Long)

    /**
     * Remove everything not reachable from the current revision
     */
    fun gc()

    // TODO maintain invariant, that having transient forks you can't change base inputs (or it immediately interrupts forked so it can't observe changed input)
    /**
     *
     */
    fun forkTransient() : DbRuntime
}

/**
 * Runtime representation of query invocation
 */
interface Frame {
    val invocations: List<QueryInvocation<*, *>>
    val maxRevision: Long
}

class QueryInvocation<P, R>(val queryDb: QueryDb<P, R>, val parameters: P) {
    fun getRevisionOfLastChange() : Long = queryDb.getRevisionOfLastChange(parameters)
}