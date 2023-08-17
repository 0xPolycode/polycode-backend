package polycode.features.payout.util.recursion

// Used to make recursion stack-safe:
// https://betterprogramming.pub/create-stack-safe-recursion-using-trampolines-in-scala-7c0ecd003fb9
sealed interface Trampoline<A> {
    companion object {
        tailrec fun <A> run(trampoline: Trampoline<A>): A =
            when (trampoline) {
                is Return -> trampoline.value
                is Suspend -> run(trampoline.call())
                is FlatMap -> {
                    val result = trampoline.result
                    val map = trampoline.map

                    when (result) {
                        is Return -> run(map(result.value))
                        is Suspend -> run(FlatMap(result.call(), map))
                        is FlatMap -> run(FlatMap(result.result) { value -> FlatMap(result.map(value), map) })
                    }
                }
            }
    }
}

data class Return<A>(val value: A) : Trampoline<A>

data class Suspend<A>(val call: () -> Trampoline<A>) : Trampoline<A>

data class FlatMap<A>(val result: Trampoline<A>, val map: (A) -> Trampoline<A>) : Trampoline<A>
