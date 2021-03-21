package salsa.frame

import salsa.DbFrame
import salsa.DbRevision
import salsa.QueryDbProvider
import salsa.QueryKey

class QueryInvocation<P>(val key: QueryKey<P, *>, val param: P) {
    fun getRevisionOfLastChange(frame: DbFrame, dbProvider: QueryDbProvider): DbRevision {
        val db = dbProvider.findDb(key)
        return db.getRevisionOfLastChange(frame, param, dbProvider)
    }
}