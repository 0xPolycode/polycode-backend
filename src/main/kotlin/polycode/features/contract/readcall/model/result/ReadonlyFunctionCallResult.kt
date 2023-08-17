package polycode.features.contract.readcall.model.result

import polycode.util.BlockNumber
import polycode.util.UtcDateTime

data class ReadonlyFunctionCallResult(
    val blockNumber: BlockNumber,
    val timestamp: UtcDateTime,
    val rawReturnValue: String,
    val returnValues: List<Any>
)
