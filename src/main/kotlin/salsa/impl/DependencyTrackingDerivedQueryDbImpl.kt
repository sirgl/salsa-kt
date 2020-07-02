package salsa.impl

import salsa.DbRuntime
import salsa.DerivedQuery
import salsa.QueryDb
import salsa.QueryInvocation

class DependencyTrackingDerivedQueryDbImpl<P, R>(
    private val runtime: DbRuntime,
    override val query: DerivedQuery<P, R>
) : QueryDb<P, R> {
    private val cache: HashMap<P, ResultInfo<R>> = HashMap()

    override fun eval(parameters: P): R {
        checkForCycle()
        runtime.addAsDependency(this, parameters)
        val info = cache.computeIfAbsent(parameters, { this.executeQuery(it) })
        val verifiedAt = info.verifiedAtRevision
        val currentRevision = runtime.revision
        if (verifiedAt == currentRevision) {
            runtime.tryUpdateMaxChangedRevision(info.changedAtRevision)
            return info.result
        }
        if (info.dependencies.all { it.changed() < verifiedAt }) {
            // no dependencies changed since last check, whether we can reuse deps or not
            runtime.tryUpdateMaxChangedRevision(info.changedAtRevision)
            info.verifiedAtRevision = currentRevision
            return info.result
        }
        val newInfo = executeQuery(parameters)
        if (newInfo.result == info.result) {
            runtime.tryUpdateMaxChangedRevision(info.changedAtRevision)
            return info.result
        }
        cache[parameters] = newInfo
        runtime.tryUpdateMaxChangedRevision(newInfo.changedAtRevision)
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

    override fun changed(parameters: P): Long {
        val resultInfo = cache[parameters] ?: error("Changed must be called only on computed query parameters")
        return resultInfo.changedAtRevision
    }
}

// TODO we can store hash here to check, whether dep has been changed
private class ResultInfo<R>(
    val result: R,
    val dependencies: List<QueryInvocation<*, *>>,
    /**
     * Revision at which DB verified that dependencies can be reused (basically, not changed)
     */
    var verifiedAtRevision: Long,
    var changedAtRevision: Long,
)