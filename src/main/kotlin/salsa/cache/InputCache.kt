package salsa.cache

import salsa.DbRevision
import salsa.Durability

interface InputCache<P, R> {
    fun save(params: P, result: MemoizedInput<R>)

    fun load(params: P) : MemoizedInput<R>
}

class MemoizedInput<R>(val value: R, val changedAtRevision: DbRevision, val durability: Durability)