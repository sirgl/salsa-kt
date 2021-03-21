package salsa

import kotlinx.coroutines.sync.withLock
import salsa.branch.DbBranch
import salsa.cache.DerivedCache
import salsa.cache.ResultData
import salsa.tracing.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.contracts.ExperimentalContracts
import kotlin.math.max

@ExperimentalContracts
class DerivedQueryDbImpl<P, R>(
    private val lock: ReentrantReadWriteLock,
    private val query: DerivedQuery<P, R>,
    private val cache: DerivedCache<P, R>,
    private val branch: DbBranch,
    private val dbProvider: QueryDbProvider
) : QueryDb<P, R> {
    override suspend fun executeQuery(frame: DbFrame, param: P): R {
        frame.addDependency(query.key, param)
        lock.read {
            val resultData = getResultDataUpdatingIfNeeded(frame, param)
            return resultData.result
        }
    }

    private suspend fun getResultDataUpdatingIfNeeded(frame: DbFrame, param: P) : ResultData<R> {
        val slot = cache.getOrStoreEmpty(param)
        slot.dataGuard.withLock {
            val resultData = slot.data

            if (resultData == null) {
                val res = doExecuteQuery(frame, param)
                slot.data = res
                return res
            }

            // Fast path: the value was changed on this revision -> no need to recompute it
            val currentRevision = branch.revision
            val currentVerifiedAt = resultData.verifiedAtRevision
            if (currentRevision == currentVerifiedAt) {
                frame.trace { QueryReused(QueryReuseType.SameRevision, query.key, param, resultData.result) }
                return resultData
            }

            // checking that inputs used by all dependencies of current revision wasn't changed after the last time we checked
            if (resultData.dependencies.all {
                    val revisionOfLastChange = it.getRevisionOfLastChange(frame, dbProvider) // here we should go till the very inputs
                    revisionOfLastChange != -1L && revisionOfLastChange <= currentVerifiedAt
                }) {
                resultData.verifiedAtRevision = currentRevision
                frame.trace { QueryReused(QueryReuseType.DependenciesNotChanged, query.key, param, resultData.result) }
                // TODO probably here we need to call storage that the revision was changed
                return resultData
            }

            val newResultData = doExecuteQuery(frame, param)
            if (newResultData.result == resultData.result) {
                // no need to update changed at, so that more queries could skip query computation
                resultData.verifiedAtRevision = currentRevision
                return resultData
            } else {
                slot.data = newResultData
            }

            return newResultData
        }
    }

    private suspend fun doExecuteQuery(frame: DbFrame, param: P): ResultData<R> {
        val (result, child) = frame.trackedChild(query.key, param) {
            query.executeQuery(it, param)
        }
        val revision = branch.revision
        return ResultData(result, child.dependencies, revision, revision)
    }

    override fun getRevisionOfLastChange(frame: DbFrame, params: P, dbProvider: QueryDbProvider): Long {
        val data = (cache.get(params) ?: return -1).data ?: return -1
        if (data.verifiedAtRevision == branch.revision) {
            return data.changedAtRevision
        }
        var maxRevision = -1L
        for (dependency in data.dependencies) {
            @Suppress("UNCHECKED_CAST") val db = dbProvider.findDb(dependency.key) as QueryDb<Any?, Any?>
            val revisionOfLastChange = db.getRevisionOfLastChange(frame, dependency.param, dbProvider)
            if (revisionOfLastChange == -1L) return -1
            maxRevision = max(maxRevision, revisionOfLastChange)
        }
        return maxRevision
    }
}