package example

import salsa.*
import salsa.impl.BasicQueryDbImpl
import salsa.impl.DbRuntimeImpl
import salsa.impl.DependencyTrackingDerivedQueryDbImpl


interface SyntaxQueryGroup {
    // TODO this is not a syntax query
    fun getManifest() : Manifest

    fun getText(fileId: FileId) : String

    fun setText(fileId: FileId, text: String)

    fun getTokens(fileId: FileId) : List<Token>

    fun getAst(fileId: FileId) : AstFile

//    fun getDecl(pathId: DeclPathId)
}

class SyntaxQueryGroupImpl(runtime: DbRuntime) : SyntaxQueryGroup {
    private val fileDb: BasicQueryDb<FileId, String> = BasicQueryDbImpl(runtime, fileQuery)
    private val tokensDb: QueryDb<FileId, List<Token>> = DependencyTrackingDerivedQueryDbImpl(runtime, TokensQuery(fileDb))
    private val astDb: QueryDb<FileId, Pair<AstFile, List<ParseError>>> = DependencyTrackingDerivedQueryDbImpl(runtime, AstQuery(tokensDb, fileDb))

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
}

fun main() {
    val syntaxQueryGroup: SyntaxQueryGroup = SyntaxQueryGroupImpl(DbRuntimeImpl())
    val runtime = DbRuntimeImpl()
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