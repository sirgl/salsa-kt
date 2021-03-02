package salsa.trace

import salsa.*
import java.util.ArrayDeque

class QueryTracingLogger(private val onRootQueryCompleted: (TraceNode) -> Unit) : EventLogger {
    private var root: TraceNode? = null
    private val stack = ArrayDeque<TraceNode>()

    override fun logEvent(event: RuntimeEvent) {
        when (event) {
            is GetBase -> {
                current().children.add(TraceNode(event.key.name, mutableListOf()))
            }
            is PushFrame -> {
                val node = TraceNode(event.key.name, mutableListOf())
                current().children.add(node)
                stack.push(node)
            }
            is PopFrame -> {
                stack.pop()
            }
            is TopLevelQueryStarted -> {
                root = TraceNode(event.name ?: "Unnamed", mutableListOf())
                stack.push(root!!)
            }
            is TopLevelQueryFinished -> {
                val old = root!!
                onRootQueryCompleted(old)
                root = null
            }
            else -> {}
        }
    }

    private fun current(): TraceNode {
        return stack.peekLast()
    }
}