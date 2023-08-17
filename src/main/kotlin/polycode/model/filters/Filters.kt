package polycode.model.filters

@JvmInline
value class AndList<T>(val list: List<T>) {
    constructor(vararg values: T) : this(values.toList())
}

@JvmInline
value class OrList<T>(val list: List<T>) {
    constructor(vararg values: T) : this(values.toList())
}

fun <T> List<String>?.parseOrListWithNestedAndLists(wrap: (String) -> T): OrList<AndList<T>> =
    OrList(this.orEmpty().map { AndList(it.split(" AND ").map(wrap)) })
