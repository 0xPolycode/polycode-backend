package polycode.util

private class ShortCircuitingException : RuntimeException() {
    companion object {
        private const val serialVersionUID: Long = -9108409224596859571L
    }
}

fun <T> T?.bind(): T {
    return this ?: throw ShortCircuitingException()
}

fun <T> shortCircuiting(block: () -> T): T? {
    return try {
        block()
    } catch (e: ShortCircuitingException) {
        null
    }
}
