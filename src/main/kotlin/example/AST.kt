package example

sealed class AstNode(val range: IntRange)

class AstFile(range: IntRange, val classes: List<Clazz>) : AstNode(range) {
}

class Clazz(range: IntRange, val name: String?, val functions: List<Function>) : AstNode(range)

class Function(val name: String, val stmts: List<Stmt>, val typeElement: TypeElement) : AstNode()

sealed class Stmt : AstNode()

class ExprStmt(val expr: Expr) : Stmt()

class BlockStmt(val stmts: List<Stmt>) : Stmt()

class IfStmt(val condition: Expr, val thenBlock: BlockStmt, val elseBlock: BlockStmt) : Stmt()

sealed class Expr : AstNode()

class BinExpr(val left: Expr, val right: Expr, val op: BinOp) : Expr()

enum class BinOp {
    Add,
    Minus,
    Mul,
    Div
}

class IntLiteral(val value: Int) : Expr()
class StringLiteral(val value: String) : Expr()

sealed class TypeElement : AstNode()

object IntTypeElement : TypeElement()