package salsa.impl

import salsa.*

class DependencyTrackingDerivedQueryDbImpl<P, R : Any>(
    private val runtime: DbRuntime,
    override val query: DerivedQuery<P, R>,
    private val cache: DerivedQueryCache<P, R>
) : QueryDb<P, R> {

    override operator fun get(parent: Frame, parameters: P): R {
        return runtime.withReadLock {
            // TODO here we should expect that there may be exception and do not immediately apply changes for runtime
            if (runtime.willHaveCycleAfterAdding(query, parameters)) {
                throw CycleException(query, parameters)
            }
            parent.addAsDependency(this, parameters)
            getValue(parent, parameters)
        }
    }

    // Under read action
    private fun getValue(parent: Frame, parameters: P): R {
        val slot: ResultSlot<R> = cache.getOrStoreEmpty(parameters)
        // TODO optimization: if data already initialized, than we can check fast that it wasn't changed and
//        val data = slot.data
//        if (data != null) {
//            // TODO here we can return after initial check
//        }


        // TODO this is not super good that we synchronize everything
        synchronized(slot) {
            val data = slot.data

            // empty slot was created, we need just to initialize it with query result after its execution
            if (data == null) {
                val resultData = executeQuery(parameters)
                runtime.logEvent {
                    CreateDerivedMemo(query.key)
                }
                parent.tryUpdateMaxChangedRevision(resultData.changedAtRevision)
                slot.data = resultData
                cache.updateWithNonNullData(parameters, slot)
                slot.notifyAll()
                return resultData.result
            }

            val verifiedAt = data.verifiedAtRevision
            val currentRevision = runtime.revision
            // checking whether we can reuse deps or not
            if (data.dependencies.all { it.getRevisionOfLastChange() < verifiedAt }) {
                runtime.logEvent {
                    DerivedMemoNotChanged(query.key)
                }
                // no dependencies changed since last check
                parent.tryUpdateMaxChangedRevision(data.changedAtRevision)
                data.verifiedAtRevision = currentRevision
                return data.result
            }
            // Some of the dependencies were changed, need to recompute
            val newData = executeQuery(parameters)

            if (newData.result == data.result) {
                runtime.logEvent {
                    DerivedMemoNotChanged(query.key)
                }
                // Despite the dependencies are changed, the result of current computation is the same. No need to update cache
                parent.tryUpdateMaxChangedRevision(newData.changedAtRevision)
                data.verifiedAtRevision = currentRevision
                return data.result
            }

            // result of the query really changed
            slot.data = newData
            cache.updateWithNonNullData(parameters, slot)
            slot.notifyAll()
            parent.tryUpdateMaxChangedRevision(newData.changedAtRevision)
            runtime.logEvent {
                DerivedMemoUpdated(query.key, newData.result)
            }
            return newData.result
        }
    }

    private fun executeQuery(parameters: P): ResultData<R> {
        val (result, child) = trackedFrame(runtime, parameters, query) { child ->
            query.doQuery(child, parameters)
        }
        val dependencies = child.invocations
        val revision = runtime.revision
        return ResultData(result, dependencies, revision, revision)
    }

    override fun getRevisionOfLastChange(parameters: P): Long {
        val slot = cache.get(parameters) ?: error("Changed must be called only on computed query parameters")
        val data: ResultData<R>? = slot.data
        if (data != null) {
            return data.changedAtRevision
        }
        synchronized(slot) {
            while (slot.data == null) {
                try {
                    slot.wait()
                } catch (ignored: InterruptedException) {}
            }
            return slot.data!!.changedAtRevision
        }
    }
}

/**
 * Slot for result of a query with given parameters (stored in cache).
 * If data is null, it is possible to wait for result (after setting up result, slot will be notified).
 */
// TODO we can store hash here to check, whether dep has been changed
@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN") // need to use await
class ResultSlot<R : Any>(
    @Volatile
    var data: ResultData<R>?
) : Object()

class ResultData<R: Any>(
    var result: R,
    val dependencies: List<QueryInvocation<*, *>>,
    /**
     * Revision at which DB verified that dependencies can be reused (basically, not changed)
     */
    var verifiedAtRevision: Long,
    var changedAtRevision: Long,
)