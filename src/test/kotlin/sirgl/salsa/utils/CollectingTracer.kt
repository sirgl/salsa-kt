package sirgl.salsa.utils

import salsa.tracing.QueryTracer
import salsa.tracing.RuntimeDbEvent
import salsa.tracing.TraceToken
import java.util.ArrayList

class CollectingTracer : QueryTracer {
    private val map = HashMap<TraceToken, MutableList<RuntimeDbEvent>>()
    private val lock = Any()

    override fun trace(token: TraceToken, event: RuntimeDbEvent) {
        synchronized(lock) {
            map.computeIfAbsent(token) { ArrayList() }.add(event)
        }
    }

    fun removeAll(): List<Pair<TraceToken, List<RuntimeDbEvent>>> {
        synchronized(lock) {
            val res = map.toList()
            map.clear()
            return res
        }
    }
}