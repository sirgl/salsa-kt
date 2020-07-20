package salsa

data class QueryKey<P, R>(val name: String) {
    @Suppress("UNCHECKED_CAST")
    fun cast(db: QueryDb<*, *>) : QueryDb<P, R>? {
        if (db.query.key == this) return db as QueryDb<P, R>
        return null
    }
}

interface Query<P, R> {
    val key: QueryKey<P, R>
}

class BasicQuery<P, R>(override val key: QueryKey<P, R>) : Query<P, R>

interface DerivedQuery<P, R> : Query<P, R> {
    /**
     * Execution can depend on other query DBs and MUST be idempotent.
     */
    fun doQuery(params: P) : R
}
