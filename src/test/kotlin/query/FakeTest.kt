package query

import salsa.BasicQuery
import salsa.DerivedQuery
import salsa.QueryDbProvider
import salsa.QueryKey
import salsa.impl.BasicQueryDbImpl
import salsa.impl.DbRuntimeImpl
import salsa.impl.DependencyTrackingDerivedQueryDbImpl
import salsa.impl.QueryDbProviderImpl

object QueryKeys {
    val fileText = QueryKey<FileId, String>("fileText")
    val ast = QueryKey<FileId, Ast>("ast")
}

@Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
class AstQuery(provider: QueryDbProvider) : DerivedQuery<FileId, Ast> {
    override val key: QueryKey<FileId, Ast>
        get() = QueryKey("ast")

    private val fileDb = provider.getQueryDb(QueryKeys.fileText)


    override fun doQuery(fileId: FileId): Ast {
        val text = fileDb[fileId]
        return Ast(text)
    }
}


fun test() {
    val runtime = DbRuntimeImpl()
    val fileTextQuery = BasicQuery<FileId, String>(QueryKey("source_text"))
    val fileTextQueryDb = BasicQueryDbImpl(runtime, fileTextQuery)
    val astQueryDb = DependencyTrackingDerivedQueryDbImpl(runtime, AstQuery(QueryDbProviderImpl()))
    val file1 = FileId(1)
    val file2 = FileId(2)

    // global revision R0
    fileTextQueryDb[file1] = "class File1 {}" // f1 -> ("...", R1), global revision R1
    fileTextQueryDb[file2] = "class File2 {}" // f2 -> ("...", R2), global revision R2
    val file1Ast1 = astQueryDb[file1] // (verified_at: R2, changed_at: R1, deps: [source_text(file1)])
    fileTextQueryDb[file2] = "class File2 { void foo() {} }" // f2 -> ("...", R3), global revision R3
    val file1Ast2 = astQueryDb[file1]
    // (verifiedAt: R3, changedAt: R1, deps: [source_text(file1)])
    // on entry:
    //   (verifiedAt: R2, changedAt: R1, deps: [source_text(file1)])
    // iterate over the dependencies:
    //   source_text(file1) -- the most recent version is R1
    // nothing changed that affects us, so we just update `verifiedAt` to R3
    //
    // (verifiedAt: R3, changedAt: R1, deps: [source_text(file1)])

}

data class FileId(val id: Int)
class Ast(val text: String)