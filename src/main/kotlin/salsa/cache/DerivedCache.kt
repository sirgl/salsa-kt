package salsa.cache

import kotlinx.coroutines.sync.Mutex
import salsa.frame.QueryInvocation


interface DerivedCache<P, R> {
    fun set(key: P, value: ResultSlot<R>)

    /**
     *  Load value from storage
     */
    fun get(key: P) : ResultSlot<R>?

    /**
     * Gets existing or returns empty slot to fill. Empty slot is remembered.
     * The same slot should be returned for the following requests to the same parameters.
     */
    fun getOrStoreEmpty(parameters: P) : ResultSlot<R>

    /**
     * After [ResultSlot.data] is filled this method should be called
     */
    fun updateWithNonNullData(parameters: P, value: ResultSlot<R>)
}


/**
 * Slot for result of a query with given parameters (stored in cache).
 * If data is null, it is possible to wait for result (after setting up result, slot will be notified).
 */
class ResultSlot<R>(
    @Volatile
    var data: ResultData<R>?,
    val dataGuard: Mutex
)

class ResultData<R>(
    var result: R,

    val dependencies: Set<QueryInvocation<*>>,

    /**
     * Revision at which it was verified that the [result] of the query can be used at the given revision
     */
    var verifiedAtRevision: Long,

    /**
     * The highest revision of change of the value for current query
     */
    var changedAtRevision: Long,
)