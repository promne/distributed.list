import java.io.Serializable

fun <K, V> Map<K, V>.mergeReduce(other: Map<K, V>, reduce: (V, V) -> V = { _, b -> b }): Map<K, V> =
        this.toMutableMap().apply { other.forEach { merge(it.key, it.value, reduce) } }

data class VectorTimestamp<T>(val clocks: Map<T, Long> = mutableMapOf()) : Serializable {
    fun increment(id: T) = VectorTimestamp(clocks.toMutableMap().apply { this[id] = clocks.getOrDefault(id, 0).inc() })

    fun merge(id: T, other: VectorTimestamp<T>) =
            VectorTimestamp(
                    clocks.toMutableMap()
                            .mergeReduce(other.clocks, Math::max)
            )

    fun isAfterOrEqual(other: VectorTimestamp<T>) = clocks.all { it.value >= other.clocks.getOrDefault(it.key, 0) }

    fun isAfter(other: VectorTimestamp<T>) = isAfterOrEqual(other) && clocks.any { it.value > other.clocks.getOrDefault(it.key, 0) }

}

class VectorClock<T>(val id: T) : Serializable {
    private var timestamp = VectorTimestamp<T>()

    fun increment(): VectorTimestamp<T> {
        synchronized(this) {
            timestamp = timestamp.increment(id)
            return timestamp
        }
    }

    fun merge(other: VectorTimestamp<T>): VectorTimestamp<T> {
        synchronized(this) {
            timestamp = timestamp.merge(id, other)
            return timestamp
        }
    }

    override fun toString(): String = timestamp.toString()

}