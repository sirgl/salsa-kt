package salsa.trace

import salsa.RuntimeEvent

interface EventLogger {
    // TODO should be thread safe
    fun logEvent(event: RuntimeEvent)
}