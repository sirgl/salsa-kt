package salsa

interface QueryDb<P, R> {
    suspend fun executeQuery(frame: DbFrame, param: P) : R

    /**
     * @return non-negative revision if required info is in the cache, -1 otherwise
     */
    suspend fun getRevisionOfLastChange(frame: DbFrame, params: P, dbProvider: QueryDbProvider) : Long
}

interface InputQueryDb<P, R> : QueryDb<P, R> {
    fun setValue(parameter: P, value: R, durability: Durability)
}