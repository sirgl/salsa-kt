package example

class Parser(
    private val tokens: List<Token>,
    private val text: String
) {
    internal var position: Int = 0
    private val errors = ArrayList<ParseError>()

    fun parse() : Pair<AstFile, List<ParseError>> {
        val file = parseFile()
        return file to errors
    }

    private fun parseFile() : AstFile {
        skipSpaces()
        val file = mark()
        val classes = ArrayList<Clazz>()
        try {
            while (at(TokenType.Class)) {
                classes.add(parseClass())
            }
            if (!atEnd()) {
                parseError("Class expected")
                unparseable()
            }
        } catch (e: TmpParseException) {}
        return AstFile(file.finish(), classes)
    }

    private fun parseClass() : Clazz {
        assert(at(TokenType.Class))
        val clazz = mark()
        advance()
        val name = tokenTextOrRecover(TokenType.LBrace, "Class name expected")

        if (at(TokenType.LBrace)) {
            advance()
        } else {
            parseError("'{' expected")
        }

        val functions = ArrayList<Function>()
        while (true) {
            if (at(TokenType.Fun)) {
                functions.add(parseFunction())
            } else {
                recoverUntil(TokenType.RPar, "Function expected")
                break
            }
        }
        if (!at(TokenType.RPar)) {
            parseError("'}' expected")
        }

        return Clazz(clazz.finish(), name, functions)
    }

    private fun parseFunction() : Function {
        assert(at(TokenType.Fun))
        val function = mark()
        advance()
        val name = tokenTextOrRecover(TokenType.LPar, "Function name expected")
        unparseable()
    }

    private fun parseStmt() : Stmt {
        unparseable()
    }

    private fun tokenTextOrRecover(recoverUntil: TokenType, errorText: String): String? {
        return if (at(TokenType.Id)) {
            val idText = tokenText()
            advance()
            idText
        } else {
            recoverUntil(recoverUntil, errorText)
            null
        }
    }

    private fun recoverUntil(tokenTypes: Set<TokenType>, errorText: String) {
        parseError(errorText)
        while (token(0).type !in tokenTypes || !atEnd()) {
            advance()
        }
    }

    private fun recoverUntil(tokenType: TokenType, errorText: String) {
        parseError(errorText)
        while (!at(tokenType) || !atEnd()) {
            advance()
        }
    }

    private fun parseError(text: String) {
        errors.add(ParseError(position, text))
    }

    private fun at(expected: TokenType) : Boolean {
        return token(0).type == expected
    }

    private fun token(offset: Int) : Token {
        return tokens[position + offset]
    }

    private fun tokenText() : String {
        val token = token(0)
        return text.substring(token.startIncl, token.endExcl)
    }

    private fun advance() {
        position++
        skipSpaces()
    }

    private fun skipSpaces() {
        while (at(TokenType.Space)) {
            position++
        }
    }

    private fun mark() : Marker {
        return Marker(position, this)
    }

    private fun atEnd() : Boolean {
        return position >= tokens.size
    }
}

fun unparseable() : Nothing = throw TmpParseException()

// Just to speed up parser construction, not to bother about parser recovery
class TmpParseException : RuntimeException()

private class Marker(val start: Int, val parser: Parser) {
    fun finish() : IntRange {
        TODO("translate token positions into offsets")
        return IntRange(start, parser.position)
    }
}

class ParseError(val position: Int, val text: String)