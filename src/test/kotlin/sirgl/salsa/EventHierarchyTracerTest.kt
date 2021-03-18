package sirgl.salsa

import org.junit.Test
import salsa.tracing.*
import sirgl.salsa.utils.Sum2Query
import sirgl.salsa.utils.intIntKey
import kotlin.test.assertEquals

class EventHierarchyTracerTest {
    @Test
    fun testTopLevelOnly() {
        val events = ArrayList<EventInfo>()
        val tracer = EventHierarchyBuildingTracer { events.add(it) }
        val token = TraceToken(1, "Top level")
        tracer.trace(token, TopLevelQueryStarted)
        tracer.trace(token, TopLevelQueryFinished)
        assertEquals(
            listOf(EventInfo(token, TraceEventNode(TopLevelQueryStarted, TopLevelQueryFinished, emptyList()))),
            events
        )
    }

    @Test
    fun testHierarchy() {
        val events = ArrayList<EventInfo>()
        val tracer = EventHierarchyBuildingTracer { events.add(it) }
        val token = TraceToken(1, "Top level")
        tracer.trace(token, TopLevelQueryStarted)
        tracer.trace(token, QueryStarted(Sum2Query.KEY, 1 to 2))
        run {
            tracer.trace(token, GetInputEvent(intIntKey, 1))
            tracer.trace(token, QueryStarted(Sum2Query.KEY, 3 to 4))
            run {
                tracer.trace(token, GetInputEvent(intIntKey, 3))
            }
            tracer.trace(token, QueryFinished(Sum2Query.KEY, 3 to 4))
        }
        tracer.trace(token, QueryFinished(Sum2Query.KEY, 1 to 2))

        tracer.trace(token, QueryStarted(Sum2Query.KEY, 2 to 3))
        run {
            tracer.trace(token, GetInputEvent(intIntKey, 2))
        }
        tracer.trace(token, QueryFinished(Sum2Query.KEY, 2 to 3))
        tracer.trace(token, TopLevelQueryFinished)
        assertEquals(
            listOf(
                EventInfo(
                    token, TraceEventNode(
                        TopLevelQueryStarted, TopLevelQueryFinished,
                        listOf(
                            TraceEventNode(
                                QueryStarted(Sum2Query.KEY, 1 to 2), QueryFinished(Sum2Query.KEY, 1 to 2), listOf(
                                    TraceEventNode(GetInputEvent(intIntKey, 1), null, emptyList()),
                                    TraceEventNode(
                                        QueryStarted(Sum2Query.KEY, 3 to 4),
                                        QueryFinished(Sum2Query.KEY, 3 to 4),
                                        listOf(
                                            TraceEventNode(GetInputEvent(intIntKey, 3), null, emptyList())
                                        )
                                    ),
                                )),
                                TraceEventNode(
                                    QueryStarted(Sum2Query.KEY, 2 to 3),
                                    QueryFinished(Sum2Query.KEY, 2 to 3),
                                    listOf(
                                        TraceEventNode(GetInputEvent(intIntKey, 2), null, emptyList())
                                    )
                                ),

                        )
                    )
                )
            ), events
        )
    }
}