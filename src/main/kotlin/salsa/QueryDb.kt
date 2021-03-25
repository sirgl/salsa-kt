package salsa

import salsa.cache.DerivedCache
import salsa.cache.InputCache

interface QueryDb<P, R> {
    suspend fun executeQuery(frame: DbFrame, param: P) : R

    /**
     * @return non-negative revision if required info is in the cache, -1 otherwise
     */
    fun getRevisionOfLastChange(frame: DbFrame, params: P, dbProvider: QueryDbProvider) : Long
}

interface DerivedQueryDb<P, R> : QueryDb<P, R> {
    val cache: DerivedCache<P, R>
    val query: DerivedQuery<P, R>
}

interface InputQueryDb<P, R> : QueryDb<P, R> {
    val cache: InputCache<P, R>

    fun setValue(parameter: P, value: R, durability: Durability)
}