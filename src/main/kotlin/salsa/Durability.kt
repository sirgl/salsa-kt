package salsa

/**
 * How long heuristically result of the query will be the same
 */
enum class Durability {
    Low,
    Medium,
    High;

    fun id() : Byte = when (this) {
        Low -> 0
        Medium -> 1
        High -> 2
    }


    companion object {
        fun from(raw: Byte) : Durability {
            return when (raw.toInt()) {
                0 -> Low
                1 -> Medium
                2 -> High
                else -> throw IllegalArgumentException("Raw durability value: $raw")
            }
        }
    }
}