package polycode.util

import org.junit.jupiter.api.Test
import polycode.TestBase
import java.math.BigInteger

class BinarySearchTest : TestBase() {

    companion object {
        private enum class SearchDirection {
            LOWER, UPPER
        }

        private fun withTarget(target: BigInteger): (BigInteger) -> SearchDirection =
            { current -> if (current > target) SearchDirection.UPPER else SearchDirection.LOWER }

        private val isLower: (SearchDirection) -> Boolean = { it == SearchDirection.LOWER }
        private val isUpper: (SearchDirection) -> Boolean = { it == SearchDirection.UPPER }
    }

    @Test
    fun binarySearchWorksCorrectlyForEvenNumberInterval() {
        val lowerBound = BigInteger("100")
        val upperBound = BigInteger("500")
        val target = BigInteger("400")

        verify("binary search will find correct value for even number interval") {
            val result = BinarySearch(
                lowerBound = lowerBound,
                upperBound = upperBound,
                getValue = withTarget(target),
                updateLowerBound = isLower,
                updateUpperBound = isUpper
            )

            expectThat(result)
                .isEqualTo(target)
        }
    }

    @Test
    fun binarySearchWorksCorrectlyForOddNumberInterval() {
        val lowerBound = BigInteger("98")
        val upperBound = BigInteger("503")
        val target = BigInteger("400")

        verify("binary search will find correct value for odd number interval") {
            val result = BinarySearch(
                lowerBound = lowerBound,
                upperBound = upperBound,
                getValue = withTarget(target),
                updateLowerBound = isLower,
                updateUpperBound = isUpper
            )

            expectThat(result)
                .isEqualTo(target)
        }
    }

    @Test
    fun binarySearchWorksCorrectlyForSameLowerAndUpperBounds() {
        val lowerBound = BigInteger("400")
        val upperBound = BigInteger("400")
        val target = BigInteger("400")

        verify("binary search will find correct value for same lower and upper bounds") {
            val result = BinarySearch(
                lowerBound = lowerBound,
                upperBound = upperBound,
                getValue = withTarget(target),
                updateLowerBound = isLower,
                updateUpperBound = isUpper
            )

            expectThat(result)
                .isEqualTo(target)
        }
    }

    @Test
    fun binarySearchWorksReturnsLowerBoundForReversedBounds() {
        val lowerBound = BigInteger("500")
        val upperBound = BigInteger("100")
        val target = BigInteger("400")

        verify("binary search will return numerically lower bound for reversed bounds") {
            val result = BinarySearch(
                lowerBound = lowerBound,
                upperBound = upperBound,
                getValue = withTarget(target),
                updateLowerBound = isLower,
                updateUpperBound = isUpper
            )

            expectThat(result)
                .isEqualTo(BigInteger("100"))
        }
    }
}
