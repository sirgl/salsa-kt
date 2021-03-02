package salsa.trace

import java.util.*

data class TraceNode(val text: String, val children: MutableList<TraceNode>)

fun toDotFormat(node: TraceNode): String {
    val nodeIds = assignIdsToNodes(node)
    return buildString {
        appendLine("digraph graphname {")
        bfs(node) {
            val id = nodeIds[it]!!
            for (child in it.children) {
                val childId = nodeIds[child]!!
                appendLine("$id -> $childId;")
            }
            appendLine("$id [label=\"${it.text}\"];")
        }
        appendLine("}")
    }
}

private fun assignIdsToNodes(node: TraceNode): Map<TraceNode, Int> {
    var counter = 0
    val nodeIds = HashMap<TraceNode, Int>()
    bfs(node) {
        nodeIds[it] = counter
        counter++
    }
    return nodeIds
}

private fun bfs(
    node: TraceNode,
    block: (TraceNode) -> Unit
) {
    val queue: Queue<TraceNode> = ArrayDeque()
    queue.add(node)
    while (queue.isNotEmpty()) {
        val current = queue.remove()
        block(current)
        for (child in current.children) {
            queue.add(child)
        }
    }
}