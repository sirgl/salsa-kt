package salsa.tracing

class DispatchingTracer(private vararg val childTracers: QueryTracer) : QueryTracer {
    override fun trace(token: TraceToken, event: RuntimeDbEvent) {
        for (tracer in childTracers) {
            tracer.trace(token, event)
        }
    }
}