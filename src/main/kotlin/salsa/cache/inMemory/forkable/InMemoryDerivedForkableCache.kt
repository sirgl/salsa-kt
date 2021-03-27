package salsa.cache.inMemory.forkable

import io.lacuna.bifurcan.IMap
import kotlinx.coroutines.sync.Mutex
import salsa.DbRevision
import salsa.ResultDataImpl
import salsa.cache.DerivedCache
import salsa.cache.ResultData
import salsa.cache.ResultSlot
import salsa.cache.inMemory.linear.ResultSlotImpl
import salsa.collections.ImmMap

class InMemoryDerivedForkableCache<P, R> private constructor(
    @Volatile private var storage : IMap<P, ResultSlot<R>>
) : DerivedCache<P, R> {
    constructor() : this(ImmMap())

    override fun get(key: P): ResultSlot<R>? {
        return storage.get(key).orElse(null)
    }

    override fun getOrStoreEmpty(parameters: P): ResultSlot<R> {
        val existing = storage.get(parameters).orElse(null)
        if (existing != null) {
            return existing
        }
        val newSlot = ResultSlotImpl<R>(dataGuard = Mutex(false), data = null)
        storage = storage.put(parameters, newSlot)
        return newSlot
    }

    override fun updateSlotData(slot: ResultSlot<R>, newData: ResultData<R>) {
        @Suppress("UNCHECKED_CAST") val data = slot as ResultSlotImpl<R>
        data.data = newData
    }

    override fun updateVerifiedAtRevision(slot: ResultSlot<R>, data: ResultData<R>, revision: DbRevision) {
        data as ResultDataImpl<R> // TODO make it type parameter
        data.verifiedAtRevision = revision
    }

    override fun fork(): DerivedCache<P, R> {
        return InMemoryDerivedForkableCache()
    }
}