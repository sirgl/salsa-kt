package salsa.impl

import salsa.DerivedQuery
import salsa.DbRuntime
import salsa.QueryDb

/**
 * Derived value changes at every revision
 */
class UnstableDerivedQueryDbImpl<P, R>(private val runtime: DbRuntime, override val query: DerivedQuery<P, R>) :
    QueryDb<P, R> {
    private val cache: HashMap<P, R> = HashMap()
    private var revision: Long = runtime.revision

    override fun eval(parameters: P): R {
        runtime.addAsDependency(this, parameters)
        if (revision != runtime.revision) {
            revision = runtime.revision
            cache.clear()
        }
        return cache.computeIfAbsent(parameters, { query.doQuery(it) })
    }

    override fun changed(parameters: P): Long {
        return runtime.revision
    }
}