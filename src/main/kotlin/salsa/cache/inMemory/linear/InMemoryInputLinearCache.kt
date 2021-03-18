package salsa.cache.inMemory.linear

import salsa.QueryKey
import salsa.cache.InputCache
import salsa.cache.MemoizedInput
import java.util.concurrent.ConcurrentHashMap

class InMemoryInputLinearCache<P, R>(private val key: QueryKey<P, R>) : InputCache<P, R> {
    private val cache = ConcurrentHashMap<P, MemoizedInput<R>>()

    override fun save(params: P, result: MemoizedInput<R>) {
        cache[params] = result
    }

    override fun load(params: P): MemoizedInput<R> {
        return cache[params] ?: error("Input $params in $key accessed before set")
    }
}