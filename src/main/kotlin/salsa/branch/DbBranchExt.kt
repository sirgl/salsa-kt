package salsa.branch

import salsa.tracing.RuntimeDbEvent
import salsa.tracing.TraceToken

inline fun DbBranch.trace(traceToken: TraceToken, eventProducer: () -> RuntimeDbEvent) {
    context.tracer?.trace(traceToken, eventProducer())
}