package salsa


/**
 * Runtime for the database, stores revision counter and [Frame] stack
 * [DbRuntime] is protected with R/W lock
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
     * Add new frame to the stack and make it active
     */
    fun <P, R> pushFrame(query: Query<P, R>, parameters: P): Frame

    /**
     * Remove previous frame from the stack and
     */
    fun popFrame(): Frame

    fun <P, R> willHaveCycleAfterAdding(query: Query<P, R>, parameters: P): Boolean

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
    fun forkTransient(): DbRuntime

    fun emitEvent(event: RuntimeEvent)

    fun hasLogger(): Boolean

    // TODO specify, what is under lock?
    fun <R> withReadLock(action: () -> R): R

    fun <R> withWriteLock(action: () -> R): R

    fun isWaitingForWrite(): Boolean

    fun createTopLevelFrame(name: String?) : Frame
}

inline fun DbRuntime.logEvent(eventBuilder: () -> RuntimeEvent) {
    if (hasLogger()) {
        val event = eventBuilder()
        emitEvent(event)
    }
}

fun DbRuntime.checkCancelled() {
    if (isWaitingForWrite()) {
        throw CancellationException()
    }
}

fun <P, R> DbRuntime.topLevelCall(db: QueryDb<P, R>, key: P, name: String? = null): R {
    logEvent { TopLevelQueryStarted(name) }
    val res = db[createTopLevelFrame(name), key]
    logEvent { TopLevelQueryFinished(name) }
    return res
}

/**
 * Runtime representation of query invocation
 */
interface Frame {
    val invocations: List<QueryInvocation<*, *>>
    val maxRevision: Long

    fun <P, R> createChildFrame(query: Query<P, R>, parameters: P): Frame

    fun tryUpdateMaxChangedRevision(revision: Long)

    fun <P, R> addAsDependency(queryDb: QueryDb<P, R>, parameters: P)

    val name: String?
}

class QueryInvocation<P, R>(val queryDb: QueryDb<P, R>, val parameters: P) {
    fun getRevisionOfLastChange(): Long = queryDb.getRevisionOfLastChange(parameters)
}

class CancellationException : Exception()