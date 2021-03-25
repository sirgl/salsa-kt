package salsa.cache.inMemory.transient

import salsa.cache.InputCache
import salsa.cache.MemoizedInput

class TransientInputCache<P, R>(private val mainCache: InputCache<P, R>) : InputCache<P, R> {
    private val transientCache = HashMap<P, MemoizedInput<R>>()

    override fun save(params: P, result: MemoizedInput<R>) {
        transientCache[params] = result
    }

    override fun load(params: P): MemoizedInput<R> {
        return transientCache[params] ?: mainCache.load(params)
    }
}