package salsa.impl

import salsa.*
import salsa.transient.TransientBaseQueryDb

class BaseQueryDbImpl<P : Any, R : Any>(private val runtime: DbRuntime, override val query: Query<P, R>) : BaseQueryDb<P, R> {
    private val inputs: HashMap<P, MemoizedBasicInput<R>> = HashMap()

    override operator fun get(parameters: P) : R {
        runtime.addAsDependency(this, parameters)
        val memoized = getMemoized(parameters)
        val value = memoized.value
        runtime.tryUpdateMaxChangedRevision(memoized.createdAt)
        runtime.logEvent { GetBase(parameters, value) }
        return value
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
        runtime.logEvent { SetBase(params, value) }
    }

    override fun getRevisionOfLastChange(parameters: P): Long {
        return getMemoized(parameters).createdAt
    }

    override fun forkTransient(): BaseQueryDb<P, R> {
        return TransientBaseQueryDb(runtime, this)
    }
}

class MemoizedBasicInput<R>(val value: R, val createdAt: Long)