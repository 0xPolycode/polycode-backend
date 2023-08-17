package polycode.features.contract.functioncall.model.params

import com.fasterxml.jackson.databind.JsonNode
import polycode.features.contract.deployment.model.params.DeployedContractIdentifier
import polycode.features.contract.functioncall.model.request.CreateContractFunctionCallRequest
import polycode.features.functions.encoding.model.FunctionArgument
import polycode.model.ScreenConfig
import polycode.util.Balance
import polycode.util.WalletAddress

data class CreateContractFunctionCallRequestParams(
    val identifier: DeployedContractIdentifier,
    val functionName: String,
    val functionParams: List<FunctionArgument>,
    val ethAmount: Balance,
    val redirectUrl: String?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig,
    val callerAddress: WalletAddress?
) {
    constructor(requestBody: CreateContractFunctionCallRequest) : this(
        identifier = DeployedContractIdentifier(requestBody),
        functionName = requestBody.functionName,
        functionParams = requestBody.functionParams,
        ethAmount = Balance(requestBody.ethAmount),
        redirectUrl = requestBody.redirectUrl,
        arbitraryData = requestBody.arbitraryData,
        screenConfig = requestBody.screenConfig ?: ScreenConfig.EMPTY,
        callerAddress = requestBody.callerAddress?.let { WalletAddress(it) }
    )
}
