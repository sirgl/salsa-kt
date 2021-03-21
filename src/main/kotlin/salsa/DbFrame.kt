package salsa

import salsa.branch.DbBranch
import salsa.frame.DbFrameImpl
import salsa.tracing.*
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface DbFrame {
    /**
     * Implementations must be safe for multiple threads to use this method.
     */
    fun <P> addDependency(key: QueryKey<P, *>, param: P)

    val branch: DbBranch

    val traceToken: TraceToken
}

inline fun DbFrame.trace(eventProducer: () -> RuntimeDbEvent) {
    branch.context.tracer?.trace(traceToken, eventProducer())
}

@ExperimentalContracts
inline fun <P, R> DbFrame.trackedChild(key: QueryKey<P, R>, params: P, b: (DbFrame) -> R) : Pair<R, DbFrameImpl> {
    contract {
        callsInPlace(b, InvocationKind.EXACTLY_ONCE)
    }
    val current = DbFrameImpl(branch, traceToken)
    trace { QueryStarted(key, params) }
    try {
        return b(current) to current
    } finally {

        val deps = current.dependencies
        trace { QueryFinished(key, params) }
    }
}