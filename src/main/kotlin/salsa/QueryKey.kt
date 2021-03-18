package salsa

sealed class QueryKey<P, R>(val id: String) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is QueryKey<*, *>) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

class DerivedQueryKey<P, R>(id: String) : QueryKey<P, R>(id) {
    override fun toString(): String {
        return "DerivedQueryKey($id)"
    }
}

class InputQueryKey<P, R>(id: String) : QueryKey<P, R>(id) {
    override fun toString(): String {
        return "InputQueryKey($id)"
    }
}

