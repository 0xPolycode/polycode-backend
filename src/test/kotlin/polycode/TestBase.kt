package polycode

import org.assertj.core.api.AbstractBigIntegerAssert
import org.assertj.core.api.AbstractBooleanAssert
import org.assertj.core.api.AbstractCollectionAssert
import org.assertj.core.api.AbstractIntegerAssert
import org.assertj.core.api.AbstractLongAssert
import org.assertj.core.api.AbstractOffsetDateTimeAssert
import org.assertj.core.api.AbstractStringAssert
import org.assertj.core.api.Assert
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.assertj.core.api.ListAssert
import org.assertj.core.api.MapAssert
import org.assertj.core.api.ObjectAssert
import org.assertj.core.data.TemporalUnitOffset
import org.junit.jupiter.api.assertThrows
import org.mockito.BDDMockito
import org.mockito.kotlin.argThat
import org.mockito.kotlin.given
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.test.context.ActiveProfiles
import java.math.BigInteger
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import org.mockito.kotlin.times as mockitoTimes
import org.mockito.kotlin.verify as verifyMock

@ActiveProfiles("test")
abstract class TestBase {

    companion object {
        val WITHIN_TIME_TOLERANCE: TemporalUnitOffset = within(5, ChronoUnit.MINUTES)

        data class MockInteractions<T : Any>(val mock: T) {
            val once: T
                get() = verifyMock(mock, mockitoTimes(1))

            val twice: T
                get() = verifyMock(mock, mockitoTimes(2))

            val Int.times: T
                get() = verifyMock(mock, mockitoTimes(this))
        }

        data class SupposeMessage(val message: String) {
            fun <T> call(mockCall: T): BDDMockito.BDDMyOngoingStubbing<T> = given(mockCall)
        }

        data class VerifyMessage(val message: String) {

            fun expectNoInteractions(vararg mock: Any) = verifyNoInteractions(*mock)

            fun <T : Any> expectInteractions(mock: T, actions: MockInteractions<T>.() -> Unit) {
                actions(MockInteractions(mock))
                verifyNoMoreInteractions(mock)
            }

            inline fun <reified T : Throwable> expectThrows(executable: () -> Unit): T =
                assertThrows(message, executable)

            fun expectThat(value: Int?): AbstractIntegerAssert<*> = assertThat(value).withMessage()
            fun expectThat(value: Long?): AbstractLongAssert<*> = assertThat(value).withMessage()
            fun expectThat(value: String?): AbstractStringAssert<*> = assertThat(value).withMessage()
            fun expectThat(value: Boolean?): AbstractBooleanAssert<*> = assertThat(value).withMessage()
            fun expectThat(value: BigInteger?): AbstractBigIntegerAssert<*> = assertThat(value).withMessage()
            fun expectThat(value: OffsetDateTime?): AbstractOffsetDateTimeAssert<*> = assertThat(value).withMessage()
            fun <K, V> expectThat(value: Map<K, V>?): MapAssert<K, V> = assertThat(value).withMessage()
            fun <T> expectThat(value: List<T>?): ListAssert<T> = assertThat(value).withMessage()
            fun <T> expectThat(
                value: Set<T>?
            ): AbstractCollectionAssert<*, MutableCollection<out T>, T, ObjectAssert<T>> =
                assertThat(value).withMessage()

            fun <T> expectThat(value: T): ObjectAssert<T> = assertThat(value).withMessage()

            private fun <A : Assert<A, B>, B> Assert<A, B>.withMessage(): A = this.`as`(message)
        }
    }

    protected fun <R> suppose(description: String, function: SupposeMessage.() -> R): R {
        return function(SupposeMessage(description))
    }

    protected fun verify(description: String, function: VerifyMessage.() -> Unit) {
        function(VerifyMessage(description))
    }

    // https://github.com/mockito/mockito-kotlin/issues/309
    protected inline fun <reified T : Any> anyValueClass(unitValue: T): T {
        argThat<T> { true }
        return unitValue
    }
}
