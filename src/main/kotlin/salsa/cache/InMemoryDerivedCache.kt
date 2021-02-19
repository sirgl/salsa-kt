package salsa.cache

import salsa.DerivedQueryCache
import salsa.impl.ResultSlot
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Function

class InMemoryDerivedCache<K : Any, V: Any> : DerivedQueryCache<K, V> {
    private val storage = ConcurrentHashMap<K, ResultSlot<V>>()

    override fun set(key: K, value: ResultSlot<V>) {
        storage[key] = value
    }

    override fun get(key: K): ResultSlot<V>? {
        return storage[key]
    }

    override fun getOrStoreEmpty(parameters: K): ResultSlot<V> {
        return storage.computeIfAbsent(parameters, { ResultSlot(data = null) })
    }

    override fun updateWithNonNullData(parameters: K, value: ResultSlot<V>) {
        assert(value.data != null)
        // do nothing, we hold it in memory anyway
    }
}