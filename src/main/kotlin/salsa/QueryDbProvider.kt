package salsa

import salsa.branch.BranchTraits
import salsa.branch.DbBranch
import salsa.cache.inMemory.transient.TransientDerivedCache
import salsa.cache.inMemory.transient.TransientInputCache
import salsa.context.QueryRegistry
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock

interface QueryDbProvider {
    fun <P, R> findDb(key: QueryKey<P, R>) : QueryDb<P, R>

    fun <P, R> findInputDb(key: InputQueryKey<P, R>) : InputQueryDb<P, R>
}

class BranchQueryDbProvider(
    private val queryRegistry: QueryRegistry,
    private val branchTraits: BranchTraits,
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
                factory.createQueryDb(branchTraits, branch)
            } as QueryDb<P, R>
    }
}

class TransientDbProvider(
    private val queryRegistry: QueryRegistry,
    private val lock: ReentrantReadWriteLock,
    private val parent: DbBranch,
    private val current: DbBranch,
) : QueryDbProvider {
    private val queryDbs = ConcurrentHashMap<QueryKey<*, *>, QueryDb<*, *>>()

    override fun <P, R> findDb(key: QueryKey<P, R>): QueryDb<P, R> {
        return getOrCreate(key)
    }

    override fun <P, R> findInputDb(key: InputQueryKey<P, R>): InputQueryDb<P, R> {
        return getOrCreate(key) as InputQueryDb<P, R>
    }

    private fun <P, R> getOrCreate(key: QueryKey<P, R>): QueryDb<P, R> {
        @Suppress("UNCHECKED_CAST")
        return queryDbs.computeIfAbsent(key) {
            when (it) {
                is InputQueryKey<*, *> -> {
                    val baseDb = parent.queryDbProvider.findInputDb(it as InputQueryKey<P, R>)
                    InputQueryDbImpl(current, TransientInputCache(baseDb.cache), it)
                }
                is DerivedQueryKey<*, *> -> {
                    it as DerivedQueryKey<P, R>
                    val baseDb = parent.queryDbProvider.findDb(it) as DerivedQueryDb<P, R>
                    val query = queryRegistry.getDerivedQuery(it, this)
                    DerivedQueryDbImpl(lock, query, TransientDerivedCache(baseDb.cache), current, this)
                }
            }

        } as QueryDb<P, R>
    }
}