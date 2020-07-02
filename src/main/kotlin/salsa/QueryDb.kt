package salsa


/**
 * storage of info associated with a given query
 */
interface QueryDb<P, R> {
    val query: Query<P, R>
    fun eval(parameters: P) : R
    fun changed(parameters: P) : Long
}

interface BasicQueryDb<P, R> : QueryDb<P, R> {
    // TODO can't set it during executing another query (or we are inside a query)
    fun set(params: P, value: R)
}