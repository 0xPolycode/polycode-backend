package polycode.model.result

data class EventInfo(val signature: String?, val arguments: List<EventArgument>)

sealed interface EventArgument {
    val name: String
}

data class EventArgumentValue(
    override val name: String,
    val value: Any
) : EventArgument

data class EventArgumentHash(
    override val name: String,
    val hash: String
) : EventArgument
