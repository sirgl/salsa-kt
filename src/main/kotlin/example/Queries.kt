@file:Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")

package example

import salsa.*

object QueryKeys {
    val FILE_TEXT: QueryKey<FileId, String> = QueryKey("file text")
    val TOKENS: QueryKey<FileId, List<Token>> = QueryKey("tokens")
    val AST: QueryKey<FileId, Pair<AstFile, List<ParseError>>> = QueryKey("AST")
}

val fileQuery: Query<FileId, String> = BasicQuery(QueryKeys.FILE_TEXT)

class TokensQuery(dbProvider: QueryDbProvider) : DerivedQuery<FileId, List<Token>> {
    private val filesDb: QueryDb<FileId, String> = dbProvider.getQueryDb(QueryKeys.FILE_TEXT)

    override val key: QueryKey<FileId, List<Token>>
        get() = QueryKeys.TOKENS

    override fun doQuery(params: FileId): List<Token> {
        return tokenize(filesDb[params])
    }
}

private class AstQuery(dbProvider: QueryDbProvider) : DerivedQuery<FileId, Pair<AstFile, List<ParseError>>> {
    override val key: QueryKey<FileId, Pair<AstFile, List<ParseError>>>
        get() = QueryKeys.AST

    private val tokensDb: QueryDb<FileId, List<Token>> = dbProvider.getQueryDb(QueryKeys.TOKENS)
    private val textDb: QueryDb<FileId, String> = dbProvider.getQueryDb(QueryKeys.FILE_TEXT)

    override fun doQuery(fileId: FileId): Pair<AstFile, List<ParseError>> {
        val tokens = tokensDb[fileId]
        val text = textDb[fileId]
        return Parser(tokens, text).parse()
    }
}



