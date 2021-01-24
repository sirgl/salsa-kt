package salsa

/**
 * Multithreaded cache for derived queries
 */
interface DerivedCache<P, R> {
    /**
     * Saves value to storage
     */
    fun save(key: P, value: R)

    /**
     *
     */
    fun load(key: P) : R?
}