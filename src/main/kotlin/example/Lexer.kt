package example

import salsa.DerivedQuery
import salsa.QueryDb
import salsa.QueryKey

fun tokenize(text: String) : List<Token> {
    val tokens = ArrayList<Token>()
    val lexer = Lexer(text)
    while (true) {
        val token = lexer.nextToken() ?: break
        tokens.add(token)
    }
    return tokens
}

@Suppress("ConvertTwoComparisonsToRangeCheck")
class Lexer(private val text: String) {
    private var position: Int = 0

    fun nextToken() : Token? {
        val startPosition = position
        val current = current() ?: return null
        val resultType = when {
            isSpace(current) -> {
                consumeWhile(this::isSpace)
                TokenType.Space
            }
            isDigit(current) -> {
                consumeWhile(this::isDigit)
                TokenType.IntLiteral
            }
            isAlpha(current) -> {
                when {
                    atWord("fun") -> {
                        consumeWord("fun")
                        TokenType.Fun
                    }
                    atWord("class") -> {
                        consumeWord("class")
                        TokenType.Class
                    }
                    else -> {
                        consumeWhile(this::isAlphaOrDigit)
                        TokenType.Id
                    }
                }
            }
            else -> {
                if (current == '\"') {
                    advance()
                    consumeWhile { it != '\"' } // TODO handle escapes
                    advance()
                    TokenType.StringLiteral
                } else {
                    val tokenType = when (current) {
                        '{' -> TokenType.LBrace
                        '}' -> TokenType.RBrace
                        '+' -> TokenType.Plus
                        '-' -> TokenType.Minus
                        '/' -> TokenType.Div
                        '*' -> TokenType.Mul
                        '(' -> TokenType.LPar
                        ')' -> TokenType.RPar
                        '.' -> TokenType.Dot
                        ';' -> TokenType.Semi
                        ':' -> TokenType.Colon
                        ',' -> TokenType.Comma
                        else -> TokenType.Error
                    }
                    advance()
                    tokenType
                }
            }
        }
        return Token(startPosition, position, resultType)
    }

    private fun isSpace(ch: Char) : Boolean {
        return ch == ' ' || ch == '\n' || ch == '\t'
    }

    private fun isDigit(ch: Char) : Boolean {
        return ch >= '0' && ch <= '9'
    }

    private fun isAlpha(ch: Char) : Boolean {
        return ch >= 'a' && ch <= 'z' || ch >= 'A' && ch <= 'Z' || ch == '_'
    }

    private fun isAlphaOrDigit(ch: Char) : Boolean {
        return isDigit(ch) || isAlpha(ch)
    }

    private fun current() : Char? {
        val index = position
        return if (index < text.length) text[index] else null
    }

    private fun advance() {
        position++
    }

    private fun consumeWord(word: String) {
        position += word.length
    }

    private fun atWord(word: String): Boolean {
        for ((offset, ch) in word.withIndex()) {
            if (text.getOrNull(position + offset) != ch) return false
        }
        return true
    }

    private inline fun consumeWhile(cond: (Char) -> Boolean) {
        while (true) {
            val ch = current() ?: return
            if (!cond(ch)) return
            advance()
        }
    }
}

class TokensQuery(private val filesDb: QueryDb<FileId, String>) : DerivedQuery<FileId, List<Token>> {
    override val key: QueryKey<FileId, List<Token>> = QueryKey("tokens")

    override fun doQuery(params: FileId): List<Token> {
        return tokenize(filesDb.eval(params))
    }
}