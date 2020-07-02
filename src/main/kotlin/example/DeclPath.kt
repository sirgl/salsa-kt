package example

class DeclPathId(private val id: Int)

class DeclPath(val segments: List<DeclPathSegment>)

class DeclPathSegment(val index: Int, val name: String?)