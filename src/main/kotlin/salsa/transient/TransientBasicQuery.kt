package salsa.transient

import salsa.Query
import salsa.QueryDb

class TransientBasicQuery<P, R>(val base: QueryDb<P, R>) : QueryDb<P, R> {
//    val overwritten:

    override val query: Query<P, R>
        get() = base.query

    override fun get(parameters: P): R {
        TODO("Not yet implemented")
    }

    override fun changed(parameters: P): Long {
        TODO("Not yet implemented")
    }

    override fun forkTransient(): QueryDb<P, R> {
        TODO("Not yet implemented")
    }
}