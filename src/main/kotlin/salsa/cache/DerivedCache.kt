package salsa.cache

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import salsa.DbRevision
import salsa.frame.QueryInvocation
import kotlin.contracts.contract


interface DerivedCache<P, R> {
    /**
     *  Load value from storage
     */
    fun get(key: P) : ResultSlot<R>?

    /**
     * Gets existing or returns empty slot to fill. Empty slot is remembered.
     * The same slot should be returned for the following requests to the same parameters.
     */
    fun getOrStoreEmpty(parameters: P) : ResultSlot<R>

    fun updateSlotData(slot: ResultSlot<R>, newData: ResultData<R>)

    fun updateVerifiedAtRevision(slot: ResultSlot<R>, data: ResultData<R>, revision: DbRevision)

    fun fork() : DerivedCache<P, R>
}

/**
 * Slot for result of a query with given parameters (stored in cache).
 * If data is null, it is possible to wait for result (after setting up result, slot will be notified).
 */
interface ResultSlot<R> {
    val data: ResultData<R>?
    val dataGuard: Mutex
}

suspend inline fun <R> ResultSlot<R>.accessData(b: (ResultData<R>?) -> Unit) {
    contract {
        callsInPlace(b)
    }
    dataGuard.withLock {
        b(data)
    }
}

interface ResultData<R> {
    val result: R

    val dependencies: Set<QueryInvocation<*>>

    /**
     * Revision at which it was verified that the [result] of the query can be used at the given revision
     */
    val verifiedAtRevision: Long

    /**
     * The highest revision of change of the value for current query
     */
    val changedAtRevision: Long
}