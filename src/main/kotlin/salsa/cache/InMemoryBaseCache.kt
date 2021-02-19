package salsa.cache

import salsa.BaseQueryCache
import salsa.impl.MemoizedBasicInput
import java.util.concurrent.ConcurrentHashMap

class InMemoryBaseCache<P : Any, R : Any> : BaseQueryCache<P, R> {
    private val storage = ConcurrentHashMap<P, MemoizedBasicInput<R>>()

    override fun get(key: P): MemoizedBasicInput<R>? {
        return storage[key]
    }

    override fun set(key: P, value: MemoizedBasicInput<R>) {
        storage[key] = value
    }
}