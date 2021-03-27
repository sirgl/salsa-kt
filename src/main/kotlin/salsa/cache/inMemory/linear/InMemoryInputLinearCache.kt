package salsa.cache.inMemory.linear

import salsa.QueryKey
import salsa.cache.InputCache
import salsa.cache.MemoizedInput

class InMemoryInputLinearCache<P, R>(private val key: QueryKey<P, R>) : InputCache<P, R> {
    // there is no need to make this map concurrent as inputs are only modified
    private val storage = HashMap<P, MemoizedInput<R>>()

    override fun save(params: P, result: MemoizedInput<R>) {
        storage[params] = result
    }

    override fun load(params: P): MemoizedInput<R> {
        return storage[params] ?: error("Input $params in $key accessed before set")
    }

    override fun fork(): InputCache<P, R> {
        throw NotImplementedError()
    }
}