package salsa.tracing

object StdoutTraceBuilder : HierarchyEventConsumer {
    override fun handleEvent(event: EventInfo) {
        val sb = StringBuilder()
        sb.prettyEvent(event)
        println(sb)
    }
}

class StringTraceBuilder(val consumer: (String) -> Unit) : HierarchyEventConsumer {
    override fun handleEvent(event: EventInfo) {
        val sb = StringBuilder()
        sb.prettyEvent(event)
        consumer(sb.toString())
    }
}

private fun StringBuilder.prettyEvent(event: EventInfo) {
    val token = event.traceToken
    append("#${token.id}")
    val name = token.name
    if (name != null) {
        append("($name)")
    }
    append(" ")
    prettyNode(event.node, 0)
}

private fun StringBuilder.prettyNode(node: TraceEventNode, level: Int) {
    space(level)
    appendLine(node.start)
    val children = node.children
    if (children.isNotEmpty()) {
        for (child in children) {
            prettyNode(child, level + 1)
        }
    }
    val end = node.end
    if (end != null) {
        space(level)
        appendLine(node.end)
    }
}

private fun StringBuilder.space(level: Int) {
    for (i in 0 until level) {
        append("  ")
    }
}
