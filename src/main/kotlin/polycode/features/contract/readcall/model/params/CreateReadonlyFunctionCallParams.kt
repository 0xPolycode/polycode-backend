package polycode.features.contract.readcall.model.params

import polycode.features.contract.deployment.model.params.DeployedContractIdentifier
import polycode.features.contract.readcall.model.request.ReadonlyFunctionCallRequest
import polycode.features.functions.encoding.model.FunctionArgument
import polycode.util.BlockNumber
import polycode.util.WalletAddress

data class CreateReadonlyFunctionCallParams(
    val identifier: DeployedContractIdentifier,
    val blockNumber: BlockNumber?,
    val functionName: String,
    val functionParams: List<FunctionArgument>,
    val outputParams: List<OutputParameter>,
    val callerAddress: WalletAddress
) {
    constructor(requestBody: ReadonlyFunctionCallRequest) : this(
        identifier = DeployedContractIdentifier(requestBody),
        blockNumber = requestBody.blockNumber?.let { BlockNumber(it) },
        functionName = requestBody.functionName,
        functionParams = requestBody.functionParams,
        outputParams = requestBody.outputParams,
        callerAddress = WalletAddress(requestBody.callerAddress)
    )
}
