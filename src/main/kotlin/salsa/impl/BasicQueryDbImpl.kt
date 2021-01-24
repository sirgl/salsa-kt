package salsa.impl

import salsa.BasicQueryDb
import salsa.Query
import salsa.DbRuntime
import salsa.transient.TransientBasicQueryDb

class BasicQueryDbImpl<P, R>(private val runtime: DbRuntime, override val query: Query<P, R>) : BasicQueryDb<P, R> {
    private val inputs: HashMap<P, MemoizedBasicInput<R>> = HashMap()

    override operator fun get(parameters: P) : R {
        runtime.addAsDependency(this, parameters)
        return (getMemoized(parameters)).value
    }

    private fun getMemoized(parameters: P) = inputs[parameters]
        ?: error("Input must be filled before requesting")

    override fun set(params: P, value: R) {
        val memoized = inputs[params]
        if (memoized != null) {
            if (memoized.value == value) return
        }
        inputs[params] = MemoizedBasicInput(value, runtime.revision)
        runtime.bumpRevision()
    }

    override fun getRevisionOfLastChange(parameters: P): Long {
        return getMemoized(parameters).createdAt
    }

    override fun forkTransient(): BasicQueryDb<P, R> {
        return TransientBasicQueryDb(runtime, this)
    }
}

class MemoizedBasicInput<R>(val value: R, val createdAt: Long)