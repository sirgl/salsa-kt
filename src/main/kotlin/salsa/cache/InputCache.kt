package salsa.cache

import salsa.DbRevision
import salsa.Durability

interface InputCache<P, R> {
    /**
     * Runs under write action
     */
    fun save(params: P, result: MemoizedInput<R>)

    /**
     * Runs under read action
     */
    fun load(params: P) : MemoizedInput<R>

    fun fork() : InputCache<P, R>
}

class MemoizedInput<R>(val value: R, val changedAtRevision: DbRevision, val durability: Durability)