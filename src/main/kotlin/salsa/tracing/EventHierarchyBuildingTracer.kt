package salsa.tracing

import java.util.HashMap

class EventHierarchyBuildingTracer(private val eventConsumer: HierarchyEventConsumer) : QueryTracer {
    private val topLevelEvents: HashMap<TraceToken, EventHierarchyBuilder> = HashMap()

    override fun trace(token: TraceToken, event: RuntimeDbEvent) {
        val builder = topLevelEvents.computeIfAbsent(token) { EventHierarchyBuilder() }

        val node = when (event) {
            is DbStartEvent -> {
                builder.startNode(event)
                null
            }
            is DbEndEvent -> builder.finishNode(event)
            else -> builder.singleNode(event)
        }
        if (node != null) {
            val eventInfo = EventInfo(token, node.complete())
            eventConsumer.handleEvent(eventInfo)
        }
    }
}

fun interface HierarchyEventConsumer {
    fun handleEvent(event: EventInfo)
}

data class EventInfo(val traceToken: TraceToken, val node: TraceEventNode)

data class TraceEventNode(val start: RuntimeDbEvent, val end: RuntimeDbEvent?, val children: List<TraceEventNode>)

private class EventHierarchyBuilder {
    private val stack: MutableList<BuilderNode> = ArrayList()

    fun startNode(event: RuntimeDbEvent) {
        stack.add(BuilderNode(event))
    }

    fun finishNode(event: RuntimeDbEvent): BuilderNode? {
        val current = stack.removeLast()
        event as DbEndEvent
        require(event.isMatchingStartEvent(current.start)) { "Bad pair: start = ${current.start} and end = $event" }
        current.end = event
        stack.lastOrNull()?.children?.add(current)
        return if (stack.isNotEmpty()) null else current
    }

    /**
     * return non-null node if it is top level and has to be handled
     */
    fun singleNode(event: RuntimeDbEvent): BuilderNode? {
        val parent = stack.lastOrNull()
        val node = BuilderNode(event)
        return if (parent != null) {
            parent.children.add(node)
            null
        } else {
            node
        }
    }
}

private class BuilderNode(val start: RuntimeDbEvent) {
    val children: MutableList<BuilderNode> = ArrayList()
    var end: RuntimeDbEvent? = null

    fun complete() : TraceEventNode {
        return if (end == null) {
            TraceEventNode(start, null, emptyList())
        } else {
            TraceEventNode(start, end, children.map { it.complete() })
        }
    }
}
