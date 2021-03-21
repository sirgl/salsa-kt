package salsa.frame

import salsa.DbFrame
import salsa.QueryKey
import salsa.branch.DbBranch
import salsa.tracing.TraceToken

class DbFrameImpl(override val branch: DbBranch, override val traceToken: TraceToken) : DbFrame {
    val dependencies: MutableSet<QueryInvocation<*>> = HashSet()

    private val lock = Any()

    override fun <P> addDependency(key: QueryKey<P, *>, param: P) {
        synchronized(lock) {
            dependencies.add(QueryInvocation(key, param))
        }
    }
}
