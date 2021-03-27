package salsa.cache.inMemory.linear

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import kotlinx.coroutines.sync.Mutex
import salsa.DbRevision
import salsa.ResultDataImpl
import salsa.cache.DerivedCache
import salsa.cache.ResultData
import salsa.cache.ResultSlot

// TODO result slot type parameter
class InMemoryDerivedLinearCache<P, R> : DerivedCache<P, R> {
    private val storage : Cache<P, ResultSlot<R>> = Caffeine.newBuilder()
        .maximumSize(1000)
        .build()

    override fun get(key: P): ResultSlot<R>? {
        return storage.getIfPresent(key)
    }

    override fun getOrStoreEmpty(parameters: P): ResultSlot<R> {
        return storage.get(parameters, { ResultSlotImpl(dataGuard = Mutex(false), data = null) })
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

    override fun fork(): DerivedCache<P, R> {
        throw NotImplementedError()
    }
}

class ResultSlotImpl<R>(
    @Volatile
    override var data: ResultData<R>?,
    override val dataGuard: Mutex
) : ResultSlot<R>