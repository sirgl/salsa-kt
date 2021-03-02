package salsa.impl

import salsa.*

class BaseQueryDbImpl<P : Any, R : Any>(
    private val runtime: DbRuntime,
    override val query: Query<P, R>,
    private val cache: BaseQueryCache<P, R>
) : BaseQueryDb<P, R> {
    override fun get(parent: Frame, parameters: P): R {
        return runtime.withReadLock {
            parent.addAsDependency(this, parameters)
            val memoized = getMemoized(parameters)
            val value = memoized.value
            parent.tryUpdateMaxChangedRevision(memoized.createdAt)
            runtime.logEvent { GetBase(parameters, value, query.key) }
            value
        }
    }

    private fun getMemoized(parameters: P): MemoizedBasicInput<R> = cache.get(parameters)
        ?: error("Input must be filled before requesting")

    override fun set(params: P, value: R) {
        runtime.withWriteLock {
            val memoized = cache.get(params)
            if (memoized != null) {
                if (memoized.value == value) return@withWriteLock
            }
            cache.set(params, MemoizedBasicInput(value, runtime.revision))
            runtime.bumpRevision()
            runtime.logEvent { SetBase(params, value) }
        }
    }

    override fun getRevisionOfLastChange(parameters: P): Long {
        return getMemoized(parameters).createdAt
    }
}

class MemoizedBasicInput<R>(val value: R, val createdAt: Long)