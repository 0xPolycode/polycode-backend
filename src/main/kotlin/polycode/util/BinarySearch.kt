package polycode.util

import java.math.BigInteger

object BinarySearch {
    operator fun <T> invoke(
        lowerBound: BigInteger,
        upperBound: BigInteger,
        getValue: (BigInteger) -> T,
        updateLowerBound: (T) -> Boolean,
        updateUpperBound: (T) -> Boolean
    ): BigInteger {
        tailrec fun find(lower: BigInteger, upper: BigInteger): BigInteger =
            if (upper - lower <= BigInteger.ONE) {
                lower.min(upper)
            } else {
                val current = (lower + upper) / BigInteger.TWO
                val value = getValue(current)

                find(
                    lower = if (updateLowerBound(value)) current else lower,
                    upper = if (updateUpperBound(value)) current else upper
                )
            }

        return find(lowerBound, upperBound)
    }
}
