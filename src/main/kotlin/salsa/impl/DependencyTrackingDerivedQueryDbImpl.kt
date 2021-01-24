package salsa.impl

import salsa.*

class DependencyTrackingDerivedQueryDbImpl<P, R : Any>(
    private val runtime: DbRuntime,
    override val query: DerivedQuery<P, R>
) : QueryDb<P, R> {
    private val cache: HashMap<P, ResultInfo<R>> = HashMap()

    override operator fun get(parameters: P) : R {
        // TODO better to use some common logic here!
        checkForCycle()
        runtime.addAsDependency(this, parameters)
        val info = cache.computeIfAbsent(parameters, { this.executeQuery(it) })
        val verifiedAt = info.verifiedAtRevision
        val currentRevision = runtime.revision
        if (verifiedAt == currentRevision) { // just computed the value, no need to validate
            runtime.logEvent {
                CreateDerivedMemo(query.key)
            }
            runtime.tryUpdateMaxChangedRevision(info.changedAtRevision)
            return info.result
        }
        // checking whether we can reuse deps or not
        if (info.dependencies.all { it.getRevisionOfLastChange() < verifiedAt }) {
            runtime.logEvent {
                DerivedMemoNotChanged(query.key)
            }
            // no dependencies changed since last check
            runtime.tryUpdateMaxChangedRevision(info.changedAtRevision)
            info.verifiedAtRevision = currentRevision
            return info.result
        }
        // Some of the dependencies were changed, need to recompute
        val newInfo = executeQuery(parameters)

        if (newInfo.result == info.result) {
            runtime.logEvent {
                DerivedMemoNotChanged(query.key)
            }
            // Despite the dependencies are changed, the result of current computation is the same. No need to update cache
            runtime.tryUpdateMaxChangedRevision(newInfo.changedAtRevision)
            info.verifiedAtRevision = currentRevision
            return info.result
        }
        cache[parameters] = newInfo
        runtime.tryUpdateMaxChangedRevision(newInfo.changedAtRevision)
        runtime.logEvent {
            DerivedMemoUpdated(query.key, newInfo.result)
        }
        return newInfo.result
    }

    private fun checkForCycle() {
//        TODO("Check that inside execution stack there were no frame with the same query and params")
    }

    private fun executeQuery(it: P): ResultInfo<R> {
        val (result, frame) = trackedFrame(runtime, it, query) {
            query.doQuery(it)
        }
        val dependencies = frame.invocations
        val revision = runtime.revision
        return ResultInfo(result, dependencies, revision, revision)
    }

    override fun getRevisionOfLastChange(parameters: P): Long {
        val resultInfo = cache[parameters] ?: error("Changed must be called only on computed query parameters")
        return resultInfo.changedAtRevision
    }

    override fun forkTransient(): QueryDb<P, R> {
        TODO("Not yet implemented")
    }
}

// TODO we can store hash here to check, whether dep has been changed
private class ResultInfo<R : Any>(
    val result: R,
    val dependencies: List<QueryInvocation<*, *>>,
    /**
     * Revision at which DB verified that dependencies can be reused (basically, not changed)
     */
    var verifiedAtRevision: Long,
    var changedAtRevision: Long,
)