package salsa.cache.inMemory.forkable

import io.lacuna.bifurcan.IMap
import salsa.branch.DbBranch
import salsa.cache.InputCache
import salsa.cache.MemoizedInput
import salsa.collections.ImmMap

class InMemoryInputForkableCache<P, R>(private val branch: DbBranch) : InputCache<P, R> {
    @Volatile
    private var map: IMap<P, MemoizedInput<R>> = ImmMap()

    override fun save(params: P, result: MemoizedInput<R>) {
        map = map.put(params, result)
    }

    override fun load(params: P): MemoizedInput<R> {
        return map[params].get()
    }
}