package salsa

import salsa.branch.BranchParams
import salsa.branch.DbBranch
import salsa.branch.DbBranchImpl
import salsa.context.DbContext
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

interface DbBranchRegistry {
    fun getActiveBranches() : Iterable<DbBranch>

    fun createEmptyBranch(branchParams: BranchParams) : DbBranch

    // From here starts internal API!

    fun registerBranch(branch: DbBranch)

    fun unregisterBranch(branch: DbBranch)
}

class DbBranchRegistryImpl(
    private val lock: ReentrantReadWriteLock,
    private val queryContext: DbContext
) : DbBranchRegistry {
    private val branches = ArrayList<DbBranch>()

    override fun getActiveBranches(): Iterable<DbBranch> {
        lock.read {
            return branches.toList()
        }
    }

    override fun createEmptyBranch(branchParams: BranchParams): DbBranch {
        val branch = DbBranchImpl(
            lock = lock,
            context = queryContext,
            branchParams = branchParams,
            name = branchParams.name,
            baseRevision = 0,
        )
        lock.write {
            registerBranch(branch)
            return branch
        }
    }

    override fun registerBranch(branch: DbBranch) {
        lock.write {
            require(branches.add(branch)) { "Branch $branch already registered" }
        }
    }

    override fun unregisterBranch(branch: DbBranch) {
        lock.write {
            require(branches.remove(branch)) { "Branch $branch already unregistered" }
        }
    }

}