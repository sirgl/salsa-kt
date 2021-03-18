package salsa.tracing


interface QueryTracer {
    /**
     * Implementation must be thread safe.
     */
    fun trace(token: TraceToken, event: RuntimeDbEvent)
}

data class TraceToken(val id: Long, val name: String?)