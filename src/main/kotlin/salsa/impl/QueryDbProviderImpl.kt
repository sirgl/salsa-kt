package salsa.impl

import salsa.QueryDb
import salsa.QueryDbProvider
import salsa.QueryKey

class QueryDbProviderImpl : QueryDbProvider {
    private val dbs: MutableMap<QueryKey<*, *>, QueryDb<*, *>> = HashMap()

    override fun <P, R> getQueryDb(key: QueryKey<P, R>): QueryDb<P, R> {
        return key.cast(dbs[key]!!)!!
    }

    fun <P, R> register(key: QueryKey<P, R>, db: QueryDb<P, R>) {
        dbs[key] = db
    }
}