package salsa.trace

import salsa.*
import java.util.ArrayDeque

class QueryTracingLogger(private val onRootQueryCompleted: (TraceNode) -> Unit) : EventLogger {
    private val stack = ArrayDeque<TraceNode>()

    override fun logEvent(event: RuntimeEvent) {
        when (event) {
            is GetBase -> {
                val parent = current()
                val current = TraceNode(event.key.name, mutableListOf())
                if (parent == null) {
                    TraceNode("Unknown", mutableListOf(current))
                } else {
                    parent.children.add(current)
                }
            }
            is PushFrame -> {
                val node = TraceNode(event.key.name, mutableListOf())
                current()?.children?.add(node)
                stack.push(node)
            }
            is PopFrame -> {
                val node = stack.pop()
                if (stack.isEmpty()) {
                    onRootQueryCompleted(node)
                }
            }
            else -> {}
        }
    }

    private fun current(): TraceNode? {
        return stack.peekLast()
    }
}