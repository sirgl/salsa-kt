package salsa.transient

import salsa.BasicQueryDb
import salsa.DbRuntime
import salsa.Query
import salsa.QueryDb
import salsa.impl.MemoizedBasicInput

class TransientBasicQueryDb<P, R>(
    private val runtime: DbRuntime,
    private val base: QueryDb<P, R>
) : BasicQueryDb<P, R> {
    private val overwritten: MutableMap<P, MemoizedBasicInput<R>> = HashMap()

    override val query: Query<P, R>
        get() = base.query

    override fun get(parameters: P): R {
        runtime.addAsDependency(this, parameters)
        return overwritten[parameters]?.value ?: base[parameters]
    }

    override fun changed(parameters: P): Long {
        TODO("Not yet implemented")
    }

    override fun forkTransient(): BasicQueryDb<P, R> {
        return TransientBasicQueryDb(runtime, this)
    }

    override fun set(params: P, value: R) {
        overwritten[params] = MemoizedBasicInput(value, runtime.revision)
        runtime.bumpRevision()
    }

    private fun getMemoized(parameters: P) : MemoizedBasicInput<R> {
        TODO()
    }
}