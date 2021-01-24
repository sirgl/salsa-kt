package salsa.transient

import salsa.*

// TODO is it different from simple runtime?
class TransientRuntime : DbRuntime {
    override val revision: Long
        get() = TODO("Not yet implemented")

    override fun bumpRevision() {
        TODO("Not yet implemented")
    }

    override fun <P, R> addAsDependency(queryDb: QueryDb<P, R>, parameters: P) {
        TODO("Not yet implemented")
    }

    override fun <P, R> pushFrame(query: Query<P, R>, parameters: P) {
        TODO("Not yet implemented")
    }

    override fun popFrame(): Frame {
        TODO("Not yet implemented")
    }

    override fun tryUpdateMaxChangedRevision(revision: Long) {
        TODO("Not yet implemented")
    }

    override fun gc() {
        TODO("Not yet implemented")
    }

    override fun forkTransient(): DbRuntime {
        TODO("Not yet implemented")
    }

    override fun emitEvent(event: RuntimeEvent) {
        TODO("Not yet implemented")
    }

    override fun hasLogger(): Boolean {
        TODO("Not yet implemented")
    }
}