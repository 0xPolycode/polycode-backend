package polycode.features.contract.arbitrarycall.model.params

import com.fasterxml.jackson.databind.JsonNode
import polycode.features.contract.arbitrarycall.model.request.CreateContractArbitraryCallRequest
import polycode.features.contract.deployment.model.params.DeployedContractIdentifier
import polycode.model.ScreenConfig
import polycode.util.Balance
import polycode.util.FunctionData
import polycode.util.WalletAddress

data class CreateContractArbitraryCallRequestParams(
    val identifier: DeployedContractIdentifier,
    val functionData: FunctionData,
    val ethAmount: Balance,
    val redirectUrl: String?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig,
    val callerAddress: WalletAddress?
) {
    constructor(requestBody: CreateContractArbitraryCallRequest) : this(
        identifier = DeployedContractIdentifier(requestBody),
        functionData = FunctionData(requestBody.functionData),
        ethAmount = Balance(requestBody.ethAmount),
        redirectUrl = requestBody.redirectUrl,
        arbitraryData = requestBody.arbitraryData,
        screenConfig = requestBody.screenConfig ?: ScreenConfig.EMPTY,
        callerAddress = requestBody.callerAddress?.let { WalletAddress(it) }
    )
}
