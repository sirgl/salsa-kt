package salsa.transient

import salsa.Query
import salsa.QueryDb

class TransientDepTrackingQueryDb<P, R>(val base: QueryDb<P, R>) : QueryDb<P, R> {

    override val query: Query<P, R>
        get() = base.query

    override operator fun get(parameters: P) : R {
        // TODO need to access to underlying storage and NOT modify it!
        TODO("Not yet implemented")
    }

    override fun getRevisionOfLastChange(parameters: P): Long {
        TODO("Not yet implemented")
    }

    override fun forkTransient(): QueryDb<P, R> {
        TODO("Not yet implemented")
    }
}