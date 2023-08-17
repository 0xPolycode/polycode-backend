package polycode.features.contract.deployment.model.params

import com.fasterxml.jackson.databind.JsonNode
import polycode.features.contract.deployment.model.request.CreateContractDeploymentRequest
import polycode.features.functions.encoding.model.FunctionArgument
import polycode.model.ScreenConfig
import polycode.util.Balance
import polycode.util.ContractId
import polycode.util.WalletAddress

data class CreateContractDeploymentRequestParams(
    val alias: String,
    val contractId: ContractId,
    val constructorParams: List<FunctionArgument>,
    val deployerAddress: WalletAddress?,
    val initialEthAmount: Balance,
    val redirectUrl: String?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig
) {
    constructor(requestBody: CreateContractDeploymentRequest) : this(
        alias = requestBody.alias,
        contractId = ContractId(requestBody.contractId),
        constructorParams = requestBody.constructorParams,
        deployerAddress = requestBody.deployerAddress?.let { WalletAddress(it) },
        initialEthAmount = Balance(requestBody.initialEthAmount),
        redirectUrl = requestBody.redirectUrl,
        arbitraryData = requestBody.arbitraryData,
        screenConfig = requestBody.screenConfig ?: ScreenConfig.EMPTY
    )
}
