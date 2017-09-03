import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VectorSpek : Spek({
    describe("a vector timestamp") {
        val timestamp = VectorTimestamp<String>(mapOf("a" to 1L, "b" to 2L, "c" to 3L))

        it("is before") {
            val other = VectorTimestamp<String>(mapOf("a" to 1L, "b" to 2L, "c" to 4L))
            assertTrue(other.isAfterOrEqual(timestamp))
            assertTrue(other.isAfter(timestamp))
        }

        it("is after") {
            val other = VectorTimestamp<String>(mapOf("a" to 1L, "b" to 2L, "c" to 2L))
            assertTrue(timestamp.isAfterOrEqual(other))
            assertTrue(timestamp.isAfter(other))
        }

    }
})