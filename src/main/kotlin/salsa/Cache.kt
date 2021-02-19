package salsa

import salsa.impl.MemoizedBasicInput
import salsa.impl.ResultSlot
import java.io.DataOutput

/**
 * Multithreaded cache for derived queries.
 *
 * All methods expected to be atomic.
 */
interface DerivedQueryCache<P, R : Any> {
    /**
     * Saves value to storage
     */
    fun set(key: P, value: ResultSlot<R>)

    /**
     *  Load value from storage
     */
    fun get(key: P) : ResultSlot<R>?

    /**
     * Gets existing or returns empty slot to fill. Empty slot is remembered.
     * The same slot should be returned for the following requests to the same parameters.
     */
    fun getOrStoreEmpty(parameters: P) : ResultSlot<R>

    /**
     * After [ResultSlot.data] is filled this method should be called
     */
    fun updateWithNonNullData(parameters: P, value: ResultSlot<R>)
}

interface SerializationDescriptor<T : Any> {
    fun save(value: T, output: DataOutput)
    fun load(input: DataOutput) : T
}

/**
 * Multithreaded cache for base queries.
 *
 * All methods expected to be atomic.
 */
interface BaseQueryCache<P: Any, R: Any> {
    fun get(key: P) : MemoizedBasicInput<R>?
    fun set(key: P, value: MemoizedBasicInput<R>)
}