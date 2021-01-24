package example

import salsa.*
import salsa.impl.BasicQueryDbImpl
import salsa.impl.DbRuntimeImpl
import salsa.impl.DependencyTrackingDerivedQueryDbImpl
import salsa.impl.QueryDbProviderImpl


interface SyntaxQueryGroup {
    fun getManifest() : Manifest

    fun getText(fileId: FileId) : String

    fun setText(fileId: FileId, text: String)

    fun getTokens(fileId: FileId) : List<Token>

    fun getAst(fileId: FileId) : AstFile

    fun forkTransient() : SyntaxQueryGroup
}

class SyntaxQueryGroupImpl(runtime: DbRuntime) : SyntaxQueryGroup {
    private val dbProvider = QueryDbProviderImpl()
    private val fileDb: BasicQueryDb<FileId, String> = BasicQueryDbImpl(runtime, fileQuery)
    init {
        dbProvider.register(fileDb)
    }
    private val tokensDb: QueryDb<FileId, List<Token>> =
        queryDb(DependencyTrackingDerivedQueryDbImpl(runtime, TokensQuery(dbProvider)))
    private val astDb: QueryDb<FileId, Pair<AstFile, List<ParseError>>> =
        queryDb(DependencyTrackingDerivedQueryDbImpl(runtime, AstQuery(dbProvider)))

    private fun <K, V> queryDb(queryDb: QueryDb<K, V>) : QueryDb<K, V> {
        dbProvider.register(queryDb)
        return queryDb
    }

    override fun getManifest(): Manifest {
        TODO("Not yet implemented")
    }

    override fun getText(fileId: FileId): String {
        return fileDb[fileId]
    }

    override fun setText(fileId: FileId, text: String) {
        fileDb[fileId] = text
    }

    override fun getTokens(fileId: FileId): List<Token> {
        return tokensDb[fileId]
    }

    override fun getAst(fileId: FileId): AstFile {
        return astDb[fileId].first
    }

    override fun forkTransient(): SyntaxQueryGroup {
        return TransientSyntaxQueryGroup(fileDb, tokensDb, astDb)
    }
}

class TransientSyntaxQueryGroup(
    fileDb: BasicQueryDb<FileId, String>,
    tokensDb: QueryDb<FileId, List<Token>>,
    astDb: QueryDb<FileId, Pair<AstFile, List<ParseError>>>
) : SyntaxQueryGroup {
    private val fileDb by lazy { fileDb.forkTransient() }
    private val tokensDb by lazy { tokensDb.forkTransient() }
    private val astDb by lazy { astDb.forkTransient() }

    override fun getManifest(): Manifest {
        TODO("Not yet implemented")
    }

    override fun getText(fileId: FileId): String {
        return fileDb[fileId]
    }

    override fun setText(fileId: FileId, text: String) {
        fileDb[fileId] = text
    }

    override fun getTokens(fileId: FileId): List<Token> {
        return tokensDb[fileId]
    }

    override fun getAst(fileId: FileId): AstFile {
        return astDb[fileId].first
    }

    override fun forkTransient(): SyntaxQueryGroup {
        return TransientSyntaxQueryGroup(fileDb, tokensDb, astDb)
    }
}

fun main() {
    val syntaxQueryGroup: SyntaxQueryGroup = SyntaxQueryGroupImpl(DbRuntimeImpl())
    val runtime = DbRuntimeImpl()
    syntaxQueryGroup.forkTransient()
        .setText(FileId(10), "asdsa")
//    val fileDb: BasicQueryDb<FileId, String> = BasicQueryDbImpl(runtime, fileQuery)
//    val file = FileId(10)
//    fileDb.set(file, "class A : Base { x : Int }")
//    val text = fileDb.eval(file)
//    val tokensDb: QueryDb<FileId, List<Token>> = DependencyTrackingDerivedQueryDbImpl(runtime, TokensQuery(fileDb))
//    val astDb: QueryDb<FileId, Pair<AstFile, List<ParseError>>> = DependencyTrackingDerivedQueryDbImpl(runtime, AstQuery(tokensDb, fileDb))
//    val node1 = astDb.eval(file)
//    fileDb.set(file, "fun foo(arg: Int) { print(arg); }")
//    val node2 = astDb.eval(file)
}