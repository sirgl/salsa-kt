package salsa.transient

import salsa.BaseQueryDb
import salsa.DbRuntime
import salsa.Query
import salsa.QueryDb
import salsa.impl.MemoizedBasicInput

class TransientBaseQueryDb<P, R>(
    private val runtime: DbRuntime,
    private val base: QueryDb<P, R>
) : BaseQueryDb<P, R> {
    private val overwritten: MutableMap<P, MemoizedBasicInput<R>> = HashMap()

    override val query: Query<P, R>
        get() = base.query

    override fun get(parameters: P): R {
        runtime.addAsDependency(this, parameters)
        return overwritten[parameters]?.value ?: base[parameters]
    }

    override fun getRevisionOfLastChange(parameters: P): Long {
        TODO("Not yet implemented")
    }

    override fun forkTransient(): BaseQueryDb<P, R> {
        return TransientBaseQueryDb(runtime, this)
    }

    override fun set(params: P, value: R) {
        overwritten[params] = MemoizedBasicInput(value, runtime.revision)
        runtime.bumpRevision()
    }

    private fun getMemoized(parameters: P) : MemoizedBasicInput<R> {
        TODO()
    }
}