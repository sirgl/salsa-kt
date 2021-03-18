package salsa

import salsa.branch.BranchParams
import salsa.branch.DbBranch
import salsa.context.QueryRegistry
import java.util.concurrent.ConcurrentHashMap

interface QueryDbProvider {
    fun <P, R> findDb(key: QueryKey<P, R>) : QueryDb<P, R>

    fun <P, R> findInputDb(key: InputQueryKey<P, R>) : InputQueryDb<P, R>
}

class BranchQueryDbProvider(
    private val queryRegistry: QueryRegistry,
    private val branchParams: BranchParams,
    private val branch: DbBranch
) : QueryDbProvider {
    private val queryDbs = ConcurrentHashMap<QueryKey<*, *>, QueryDb<*, *>>()

    override fun <P, R> findDb(key: QueryKey<P, R>): QueryDb<P, R> {
        return getOrCreate(key)
    }

    override fun <P, R> findInputDb(key: InputQueryKey<P, R>): InputQueryDb<P, R> {
        @Suppress("UNCHECKED_CAST")
        return getOrCreate(key) as InputQueryDb<P, R>
    }

    private fun <P, R> getOrCreate(key: QueryKey<P, R>): QueryDb<P, R> {
        @Suppress("UNCHECKED_CAST")
        return queryDbs.computeIfAbsent(key) {
                val factory = queryRegistry.getQueryDbFactory(this, it)
                factory.createQueryDb(branchParams, branch)
            } as QueryDb<P, R>
    }
}