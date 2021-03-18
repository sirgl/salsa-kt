package salsa.context

import salsa.*
import salsa.branch.BranchParams
import salsa.branch.DbBranch
import salsa.cache.inMemory.linear.InMemoryDerivedLinearCache
import salsa.cache.inMemory.linear.InMemoryInputLinearCache
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.contracts.ExperimentalContracts


interface QueryRegistry {
    fun <P, R> getQueryDbFactory(dbProvider: QueryDbProvider, key: QueryKey<P, R>) : QueryDbFactory<P, R>

    fun <P, R> registerDerivedQuery(key: QueryKey<P, R>, queryFactory: QueryFactory<P, R>, dbFactory: QueryDbFactory<P, R>? = null)

    fun <P, R> registerInputQuery(key: QueryKey<P, R>, dbFactory: QueryDbFactory<P, R>? = null)

//    fun iterateQueryKeys() : Iterable<QueryKey<*, *>>
}

@ExperimentalContracts
class QueryRegistryImpl(
    private val lock: ReentrantReadWriteLock
) : QueryRegistry {
    private val queries: MutableMap<QueryKey<*, *>, InputQueryInfo<*, *>> = HashMap()

    @Suppress("UNCHECKED_CAST")
    override fun <P, R> getQueryDbFactory(dbProvider: QueryDbProvider, key: QueryKey<P, R>): QueryDbFactory<P, R> {
        lock.read {
            val queryInfo = queries[key] ?: error("Query '${key.id}' not registered")
            val factory: QueryDbFactory<*, *>? = queryInfo.dbFactory
            return if (factory == null) {
                when (key) {
                    is DerivedQueryKey -> {
                        QueryDbFactory { params, branch ->
                            // TODO how to create query itself?
                            val queryFactory = queryInfo.queryFactory ?: error("Derived query has no factory")
                            val query = queryFactory.createQuery(dbProvider) as DerivedQuery<P, R>
                            DerivedQueryDbImpl(lock, query, InMemoryDerivedLinearCache(), branch, dbProvider)
                        }
                    }
                    is InputQueryKey -> {
                        QueryDbFactory { _, branch ->
                            // TODO decide what cache is required
                            InputQueryDbImpl(branch, InMemoryInputLinearCache(key), key)
                        }
                    }
                }
            } else {
                factory as QueryDbFactory<P, R>
            }
        }
    }

    override fun <P, R> registerDerivedQuery(
        key: QueryKey<P, R>,
        queryFactory: QueryFactory<P, R>,
        dbFactory: QueryDbFactory<P, R>?
    ) {
        lock.write {
            queries[key] = InputQueryInfo(key, queryFactory, dbFactory)
        }
    }

    override fun <P, R> registerInputQuery(key: QueryKey<P, R>, dbFactory: QueryDbFactory<P, R>?) {
        lock.write {
            queries[key] = InputQueryInfo(key, null, dbFactory)
        }
    }

}

private class InputQueryInfo<P, R>(
    val queryKey: QueryKey<P, R>,
    val queryFactory: QueryFactory<P, R>?,
    val dbFactory: QueryDbFactory<P, R>?
)


fun interface QueryDbFactory<P, R> {
    // TODO how to create single DB for several queries
    fun createQueryDb(branchParams: BranchParams, branch: DbBranch) : QueryDb<P, R>
}

fun interface QueryFactory<P, R> {
    fun createQuery(dbProvider: QueryDbProvider) : DerivedQuery<P, R>
}