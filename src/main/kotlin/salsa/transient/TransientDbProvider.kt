package salsa.transient

import salsa.QueryDb
import salsa.QueryDbProvider
import salsa.QueryKey

class TransientDbProvider(private val delegate: QueryDbProvider) : QueryDbProvider {
    private val transientDbs: MutableMap<QueryKey<*, *>, QueryDb<*, *>> = HashMap()

    override fun <P, R> getQueryDb(key: QueryKey<P, R>): QueryDb<P, R> {
        val db = transientDbs.computeIfAbsent(key) { delegate.getQueryDb(key).forkTransient() }
        return key.cast(db)!!
    }
}