package polycode.util

import polycode.features.contract.abi.model.AddressType
import polycode.features.contract.abi.model.UintType
import polycode.model.DeserializableEvent
import polycode.model.DeserializableEventInput

object PredefinedEvents {
    val ERC20_TRANSFER = DeserializableEvent(
        signature = "Transfer(address,address,uint256)",
        inputsOrder = listOf("from", "to", "value"),
        indexedInputs = listOf(
            DeserializableEventInput("from", AddressType),
            DeserializableEventInput("to", AddressType)
        ),
        regularInputs = listOf(
            DeserializableEventInput("value", UintType)
        )
    )
    val ERC20_APPROVAL = DeserializableEvent(
        signature = "Approval(address,address,uint256)",
        inputsOrder = listOf("owner", "spender", "value"),
        indexedInputs = listOf(
            DeserializableEventInput("owner", AddressType),
            DeserializableEventInput("spender", AddressType)
        ),
        regularInputs = listOf(
            DeserializableEventInput("value", UintType)
        )
    )
}
