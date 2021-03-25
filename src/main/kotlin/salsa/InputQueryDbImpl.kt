package salsa

import salsa.branch.DbBranch
import salsa.cache.InputCache
import salsa.cache.MemoizedInput
import salsa.tracing.GetInputEvent


class InputQueryDbImpl<P, R>(
    private val branch: DbBranch,
    override val cache: InputCache<P, R>,
    private val key: InputQueryKey<P, R>
) : InputQueryDb<P, R> {
    override suspend fun executeQuery(frame: DbFrame, param: P): R {
        frame.addDependency(key, param)
        frame.trace {
            GetInputEvent(key, param)
        }
        val memoizedInput = cache.load(param)
        return memoizedInput.value
    }

    override fun setValue(parameter: P, value: R, durability: Durability) {
        cache.save(parameter, MemoizedInput(value, branch.revision + 1, durability))
    }

    override fun getRevisionOfLastChange(frame: DbFrame, params: P, dbProvider: QueryDbProvider): Long {
        return cache.load(params).changedAtRevision
    }
}