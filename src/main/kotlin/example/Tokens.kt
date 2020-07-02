package example

enum class TokenType {
    Error,
    Id,
    StringLiteral,
    IntLiteral,
    Plus,
    Minus,
    Div,
    Mul,
    LBrace,
    RBrace,
    Fun,
    Class,
    Colon,
    Semi,
    LPar,
    RPar,
    Dot,
    Comma,
    Space,
}

class Token(val startIncl: Int, val endExcl: Int, val type: TokenType)