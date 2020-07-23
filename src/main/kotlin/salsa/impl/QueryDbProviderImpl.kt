package salsa.impl

import salsa.QueryDb
import salsa.QueryDbProvider
import salsa.QueryKey

class QueryDbProviderImpl : QueryDbProvider {
    private val dbs: MutableMap<QueryKey<*, *>, QueryDb<*, *>> = HashMap()

    override fun <P, R> getQueryDb(key: QueryKey<P, R>): QueryDb<P, R> {
        val db = dbs[key]!!
        return key.cast(db)!!
    }

    fun <P, R> register(db: QueryDb<P, R>) {
        dbs[db.query.key] = db
    }
}