package salsa.impl

import salsa.DbRuntime
import salsa.Frame
import salsa.Query

/**
 * Tracks all the dependencies (calls of queries) during execution of [b]
 */
inline fun <P, R> trackedFrame(runtime: DbRuntime, parameters: P, query: Query<P, R>, b: (Frame) -> R): Pair<R, Frame> {
    val frame: Frame = runtime.pushFrame(query, parameters)
    val result: R
    try {
        result = b(frame)
    } finally {
        runtime.popFrame()
    }
    return result to frame
}