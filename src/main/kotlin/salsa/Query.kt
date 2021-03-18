package salsa

interface Query<P, R> {
    val key: QueryKey<P, R>
}

class InputQuery<P, R>(override val key: InputQueryKey<P, R>) : Query<P, R>

interface DerivedQuery<P, R> : Query<P, R> {
    override val key: DerivedQueryKey<P, R>

    suspend fun executeQuery(frame: DbFrame, params: P) : R
}

