package salsa.cache.inMemory.linear

import kotlinx.coroutines.sync.Mutex
import salsa.DbRevision
import salsa.ResultDataImpl
import salsa.cache.DerivedCache
import salsa.cache.ResultData
import salsa.cache.ResultSlot
import java.util.concurrent.ConcurrentHashMap

// TODO result slot type parameter
class InMemoryDerivedLinearCache<P, R> : DerivedCache<P, R> {
    private val storage = ConcurrentHashMap<P, ResultSlot<R>>()

    override fun get(key: P): ResultSlot<R>? {
        return storage[key]
    }

    override fun getOrStoreEmpty(parameters: P): ResultSlot<R> {
        return storage.computeIfAbsent(parameters, { ResultSlotImpl(dataGuard = Mutex(false), data = null) })
    }

    override fun updateSlotData(slot: ResultSlot<R>, newData: ResultData<R>) {
        // TODO make it type parameter
        @Suppress("UNCHECKED_CAST") val data = slot as ResultSlotImpl<R>
        data.data = newData
    }

    @Suppress("UNCHECKED_CAST")
    override fun updateVerifiedAtRevision(slot: ResultSlot<R>, data: ResultData<R>, revision: DbRevision) {
        data as ResultDataImpl<R> // TODO make it type parameter
        data.verifiedAtRevision = revision
    }
}

class ResultSlotImpl<R>(
    @Volatile
    override var data: ResultData<R>?,
    override val dataGuard: Mutex
) : ResultSlot<R>