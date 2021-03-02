package salsa

sealed class RuntimeEvent

data class SetBase(val parameters: Any, val value: Any) : RuntimeEvent()

data class GetBase(val parameters: Any, val value: Any, val key: QueryKey<*, *>) : RuntimeEvent()

data class BumpRevision(val currentRevision: Long) : RuntimeEvent()

data class CreateDerivedMemo(val key: QueryKey<*, *>) : RuntimeEvent()

data class DerivedMemoNotChanged(val key: QueryKey<*, *>) : RuntimeEvent()

data class DerivedMemoUpdated(val key: QueryKey<*, *>, val value: Any) : RuntimeEvent()

data class TopLevelQueryStarted(val name: String?) : RuntimeEvent()

data class TopLevelQueryFinished(val name: String?) : RuntimeEvent()

data class PopFrame(val key: QueryKey<*, *>, val maxRevision: Long) : RuntimeEvent()

data class PushFrame(val key: QueryKey<*, *>) : RuntimeEvent()