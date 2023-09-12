package polycode.util

sealed interface Either<out L, out R> {
    fun <T> mapLeft(fn: (L) -> T): T? = null
    fun <T> mapRight(fn: (R) -> T): T? = null
}

data class Left<out L>(val value: L) : Either<L, Nothing> {
    override fun <T> mapLeft(fn: (L) -> T): T? = fn(value)
}

data class Right<out R>(val value: R) : Either<Nothing, R> {
    override fun <T> mapRight(fn: (R) -> T): T? = fn(value)
}
