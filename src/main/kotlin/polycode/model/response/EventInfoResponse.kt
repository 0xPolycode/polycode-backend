package polycode.model.response

import polycode.model.result.EventArgument
import polycode.model.result.EventArgumentHash
import polycode.model.result.EventArgumentValue
import polycode.model.result.EventInfo

data class EventInfoResponse(val signature: String?, val arguments: List<EventArgumentResponse>) {
    constructor(eventInfo: EventInfo) : this(
        signature = eventInfo.signature,
        arguments = eventInfo.arguments.map { EventArgumentResponse(it) }
    )
}

enum class EventArgumentResponseType {
    VALUE, HASH
}

data class EventArgumentResponse(
    val name: String,
    val type: EventArgumentResponseType,
    val value: Any?,
    val hash: String?
) {
    constructor(eventArgument: EventArgument) : this(
        name = eventArgument.name,
        type = when (eventArgument) {
            is EventArgumentValue -> EventArgumentResponseType.VALUE
            is EventArgumentHash -> EventArgumentResponseType.HASH
        },
        value = (eventArgument as? EventArgumentValue)?.value,
        hash = (eventArgument as? EventArgumentHash)?.hash
    )
}
