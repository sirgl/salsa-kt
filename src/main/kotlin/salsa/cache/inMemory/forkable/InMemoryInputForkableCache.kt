package salsa.cache.inMemory.forkable

import io.lacuna.bifurcan.IMap
import salsa.cache.InputCache
import salsa.cache.MemoizedInput
import salsa.collections.ImmMap

class InMemoryInputForkableCache<P, R> private constructor(
    @Volatile private var map: IMap<P, MemoizedInput<R>>
) : InputCache<P, R> {
    constructor() : this(ImmMap())

    override fun save(params: P, result: MemoizedInput<R>) {
        map = map.put(params, result)
    }

    override fun load(params: P): MemoizedInput<R> {
        return map[params].get()
    }

    override fun fork(): InputCache<P, R> {
        return InMemoryInputForkableCache(map)
    }
}