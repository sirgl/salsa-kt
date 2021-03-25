package sirgl.salsa.utils

import salsa.*
import salsa.branch.AtomInputChange
import salsa.branch.BranchParams
import salsa.branch.DbBranch
import salsa.context.DbBranchStorage
import salsa.context.DbContextImpl
import kotlin.contracts.ExperimentalContracts

val intStringKey = InputQueryKey<Int, String>("test.int.string")
val intIntKey = InputQueryKey<Int, Int>("test.int.int")

@ExperimentalContracts
fun defaultContext() = DbContextImpl(object : DbBranchStorage {})

fun defaultParams() = BranchParams(
    isDurable = false,
    isLinear = true,
    name = "main"
)

fun <P, R> DbBranch.setInput(inputKey: InputQueryKey<P, R>, params: P, result: R) {
    applyInputDiff(listOf(AtomInputChange(inputKey, params, result, Durability.High)))
}

/**
 * Sums inputs identified by key parameters
 */
class Sum2Query(
    dbProvider: QueryDbProvider,
    inputKey: InputQueryKey<Int, Int>
) : DerivedQuery<Pair<Int, Int>, Int> {
    companion object {
        @JvmStatic
        val KEY = DerivedQueryKey<Pair<Int, Int>, Int>("test.sum.2")
    }

    override val key: DerivedQueryKey<Pair<Int, Int>, Int>
        get() = KEY

    private val inputDb = dbProvider.findDb(inputKey)

    override suspend fun executeQuery(frame: DbFrame, params: Pair<Int, Int>): Int {
        val first = inputDb.executeQuery(frame, params.first)
        val second = inputDb.executeQuery(frame, params.second)
        return first + second
    }
}

class Sum3Query(
    dbProvider: QueryDbProvider,
    inputKey: InputQueryKey<Int, Int>
) : DerivedQuery<List<Int>, Int> {
    companion object {
        @JvmStatic
        val KEY = DerivedQueryKey<List<Int>, Int>("test.sum.3")
    }

    override val key: DerivedQueryKey<List<Int>, Int>
        get() = KEY

    private val sumDb = dbProvider.findDb(Sum2Query.KEY)
    private val inputDb = dbProvider.findDb(inputKey)

    override suspend fun executeQuery(frame: DbFrame, params: List<Int>): Int {
        require(params.size == 3)
        val sum2 = sumDb.executeQuery(frame, params[0] to params[1])
        return sum2 + inputDb.executeQuery(frame, params[2])
    }
}