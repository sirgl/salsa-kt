package example

import salsa.BasicQuery
import salsa.Query
import salsa.QueryKey

data class FileId(val internal: Int)

val fileQuery: Query<FileId, String> = BasicQuery(QueryKey("file text"))