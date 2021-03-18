package salsa.cache.inMemory.linear

import salsa.cache.DerivedCache
import salsa.cache.ResultSlot
import java.util.concurrent.ConcurrentHashMap

class InMemoryDerivedLinearCache<P, R> : DerivedCache<P, R> {
    private val storage = ConcurrentHashMap<P, ResultSlot<R>>()

    override fun set(key: P, value: ResultSlot<R>) {
        storage[key] = value
    }

    override fun get(key: P): ResultSlot<R>? {
        return storage[key]
    }

    override fun getOrStoreEmpty(parameters: P): ResultSlot<R> {
        return storage.computeIfAbsent(parameters, { ResultSlot(data = null) })
    }

    override fun updateWithNonNullData(parameters: P, value: ResultSlot<R>) {
        assert(value.data != null)
        // do nothing, we hold it in memory anyway
    }
}