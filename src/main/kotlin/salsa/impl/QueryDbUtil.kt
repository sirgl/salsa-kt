package salsa.impl

import salsa.DbRuntime
import salsa.Frame
import salsa.Query

/**
 * Tracks all the dependencies (calls of queries) during execution of [b]
 */
inline fun <P, R> trackedFrame(runtime: DbRuntime, parameters: P, query: Query<P, R>, b: () -> R): Pair<R, Frame> {
    runtime.pushFrame(query, parameters)
    val result: R
    val frame: Frame
    try {
        result = b()
    } finally {
        frame = runtime.popFrame()
    }
    return result to frame
}