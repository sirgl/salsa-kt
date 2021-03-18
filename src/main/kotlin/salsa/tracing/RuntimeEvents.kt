package salsa.tracing

import salsa.InputQueryKey
import salsa.QueryKey

interface DbStartEvent
interface DbEndEvent {
    fun isMatchingStartEvent(event: RuntimeDbEvent) : Boolean
}
interface TopLevelEvent

sealed class RuntimeDbEvent

data class GetInputEvent<P>(val key: InputQueryKey<P, *>, val params: P) : RuntimeDbEvent() {
    override fun toString(): String {
        return "GetInputEvent($key, $params)"
    }
}

/**
 * It is expected to be paired with [TopLevelQueryFinished] or [TopLevelQueryAborted]
 * 
 */
object TopLevelQueryStarted : RuntimeDbEvent(), DbStartEvent, TopLevelEvent {
    override fun toString(): String = "TopLevelQueryStarted"
}

object TopLevelQueryFinished : RuntimeDbEvent(), DbEndEvent, TopLevelEvent {
    override fun isMatchingStartEvent(event: RuntimeDbEvent): Boolean = event is TopLevelQueryStarted

    override fun toString(): String = "TopLevelQueryFinished"
}

object TopLevelQueryAborted : RuntimeDbEvent(), DbEndEvent, TopLevelEvent {
    override fun isMatchingStartEvent(event: RuntimeDbEvent): Boolean = event is TopLevelQueryStarted

    override fun toString(): String = "TopLevelQueryAborted"
}

data class InputsUpdate(val updated: List<Pair<InputQueryKey<*, *>, Any?>>) : RuntimeDbEvent()

data class QueryStarted<P>(val key: QueryKey<P, *>, val params: P) : RuntimeDbEvent(), DbStartEvent {
    override fun toString(): String = "QueryStarted($key, $params)"
}

data class QueryFinished<P>(val key: QueryKey<P, *>, val params: P) : RuntimeDbEvent(), DbEndEvent {
    override fun isMatchingStartEvent(event: RuntimeDbEvent): Boolean = event is QueryStarted<*> && event.key == key
    override fun toString(): String = "QueryFinished($key, $params)"
}

enum class QueryReuseType {
    SameRevision,
    DependenciesNotChanged
}

data class QueryReused<P, R>(val queryReuseType: QueryReuseType, val key: QueryKey<P, R>, val params: P, val result: R) : RuntimeDbEvent() {
    override fun toString(): String = "QueryReused($queryReuseType, $key, $params -> $result)"
}

object DependenciesCheckStarted : RuntimeDbEvent(), DbStartEvent {
    override fun toString(): String = "DependenciesCheckStarted"
}
object DependenciesCheckFinished : RuntimeDbEvent(), DbEndEvent {
    override fun toString(): String = "DependenciesCheckFinished"
    override fun isMatchingStartEvent(event: RuntimeDbEvent): Boolean {
        return event is DependenciesCheckStarted
    }
}