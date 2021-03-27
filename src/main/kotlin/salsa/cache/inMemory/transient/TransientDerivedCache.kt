package salsa.cache.inMemory.transient

import kotlinx.coroutines.sync.Mutex
import salsa.DbRevision
import salsa.ResultDataImpl
import salsa.cache.DerivedCache
import salsa.cache.ResultData
import salsa.cache.ResultSlot
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class TransientDerivedCache<P, R>(private val baseCache: DerivedCache<P, R>) : DerivedCache<P, R> {
    private val storage = HashMap<P, TransientResultSlot<R>>()
    private val lock = ReentrantLock() // TODO use StampedLock instead

    override fun get(key: P): ResultSlot<R>? {
        return lock.withLock {
            storage[key] ?: baseCache.get(key)
        }
    }

    // TODO probably it is better to copy value to this table and always create mutexes ourselves?
    override fun getOrStoreEmpty(parameters: P): ResultSlot<R> { // TODO verify that we sync always over single mutex (not possible that 2 threads synced over different mutexes)
        lock.withLock {
            val transientSlot = storage[parameters]
            if (transientSlot != null) return transientSlot
            val baseSlot = baseCache.get(parameters)
            if (baseSlot != null) {
                // We can't give base slot because in this case we may sync on wrong mutex. Mutex always belongs to single db.
                val baseData = baseSlot.data
                val copiedSlot = TransientResultSlot(baseSlot, lock,  baseData, Mutex(false))
                storage[parameters] = copiedSlot
                return copiedSlot
            }
            val emptySlot = TransientResultSlot<R>(null,  lock, null, Mutex(false))
            storage[parameters] = emptySlot
            return emptySlot
        }
    }

    override fun updateSlotData(slot: ResultSlot<R>, newData: ResultData<R>) {
        lock.withLock {
            slot as TransientResultSlot
            slot.ownData = newData
        }
    }

    override fun updateVerifiedAtRevision(slot: ResultSlot<R>, data: ResultData<R>, revision: DbRevision) {
        lock.withLock {
            slot as TransientResultSlot
            val ownData = slot.ownData
            if (ownData == null) {
                val delegateData = slot.base!!.data!!
                slot.ownData = ResultDataImpl(delegateData.result, delegateData.dependencies, revision, delegateData.changedAtRevision)
            } else {
                ownData as ResultDataImpl
                ownData.verifiedAtRevision = revision
            }
        }
    }

    override fun fork(): DerivedCache<P, R> {
        throw NotImplementedError()
    }
}

// accessed under the lock
class TransientResultSlot<R>(
    val base: ResultSlot<R>?,
    private val lock: ReentrantLock = ReentrantLock(),
    var ownData: ResultData<R>?,
    override val dataGuard: Mutex
) : ResultSlot<R> {
    override val data: ResultData<R>?
        get() {
            lock.withLock {
                return if (ownData != null) {
                    ownData
                } else {
                    base?.data
                }
            }
        }
}