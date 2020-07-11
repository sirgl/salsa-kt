package example

sealed class AstNode(val range: IntRange)

class AstFile(range: IntRange, val classes: List<Clazz>) : AstNode(range) {
}

class Clazz(range: IntRange, val name: String?, val functions: List<Function>) : AstNode(range)

class Function(range: IntRange, val name: String, val stmts: List<Stmt>, val typeElement: TypeElement) : AstNode(range)

sealed class Stmt(range: IntRange) : AstNode(range)

class ExprStmt(range: IntRange, val expr: Expr) : Stmt(range)

class BlockStmt(range: IntRange, val stmts: List<Stmt>) : Stmt(range)

class IfStmt(range: IntRange, val condition: Expr, val thenBlock: BlockStmt, val elseBlock: BlockStmt) : Stmt(range)

sealed class Expr(range: IntRange) : AstNode(range)

class BinExpr(range: IntRange, val left: Expr, val right: Expr, val op: BinOp) : Expr(range)

enum class BinOp {
    Add,
    Minus,
    Mul,
    Div
}

class IntLiteral(range: IntRange, val value: Int) : Expr(range)
class StringLiteral(range: IntRange, val value: String) : Expr(range)

sealed class TypeElement(range: IntRange) : AstNode(range)

class IntTypeElement(range: IntRange) : TypeElement(range)