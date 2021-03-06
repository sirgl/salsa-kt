package salsa.branch

import salsa.*
import salsa.context.DbContext


/**
 * Environment for queries. Can be forked.
 *
 * Branch may be cancelled, after this its storage is freed and it is not able to handle [executeQuery]. All started queries
 * will be cancelled.
 *
 * Branch may be in a frozen state after [forkTransientAndFreeze] and in this case user is not able to change its inputs until
 * it is not unfrozen.
 */
interface DbBranch {
    val context: DbContext

    fun cancel()

    /**
     * Delete branch. It should have no forks, otherwise throws exception
     */
    fun delete()

    fun isCancelled() : Boolean

    fun isDeleted() : Boolean

    /**
     * Freezes self and creates new branch.
     * For mutable storage it is better than full fork but in case of immutable works exactly the same and not freezes
     */
    fun forkTransientAndFreeze(branchParams: BranchParams) : DbBranch

    fun isFrozen() : Boolean

    val canFork: Boolean

    val revision: DbRevision

    fun fork(params: BranchParams) : DbBranch

    // TODO this method is not the best way - here we do hash lookup to find db for query
    //  probably it is better to make some kind of QueryCallSite that will already have it inside
    suspend fun <P, R> executeQuery(key: QueryKey<P, R>, params: P, name: String) : R

    /**
     * Immediately cancels all queries, applies diff and resumes
     */
    fun applyInputDiff(diff: List<AtomInputChange<*, *>>, name: String? = null)

    // TODO get rid of implementation details
    val queryDbProvider: QueryDbProvider
}

class BranchFrozenException : Exception()
class BranchDeletedException : Exception()

// TODO it would be good to have also a diff of the result change (e.g. for files)
class AtomInputChange<P, R>(
    val key: InputQueryKey<P, R>,
    val params: P,
    val result: R,
    val durability: Durability
) {
    fun applyChange(dbProvider: QueryDbProvider) {
        val db = dbProvider.findInputDb(key)
        db.setValue(params, result, durability)
    }
}

class BranchParams(
    val name: String? = null,
)

class BranchTraits(val params: BranchParams)