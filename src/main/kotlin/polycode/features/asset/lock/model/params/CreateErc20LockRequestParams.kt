package polycode.features.asset.lock.model.params

import com.fasterxml.jackson.databind.JsonNode
import polycode.features.asset.lock.model.request.CreateErc20LockRequest
import polycode.model.ScreenConfig
import polycode.util.Balance
import polycode.util.ContractAddress
import polycode.util.DurationSeconds
import polycode.util.WalletAddress

data class CreateErc20LockRequestParams(
    val redirectUrl: String?,
    val tokenAddress: ContractAddress,
    val tokenAmount: Balance,
    val lockDuration: DurationSeconds,
    val lockContractAddress: ContractAddress,
    val tokenSenderAddress: WalletAddress?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig
) {
    constructor(requestBody: CreateErc20LockRequest) : this(
        redirectUrl = requestBody.redirectUrl,
        tokenAddress = ContractAddress(requestBody.tokenAddress),
        tokenAmount = Balance(requestBody.amount),
        lockDuration = DurationSeconds(requestBody.lockDurationInSeconds),
        lockContractAddress = ContractAddress(requestBody.lockContractAddress),
        tokenSenderAddress = requestBody.senderAddress?.let { WalletAddress(it) },
        arbitraryData = requestBody.arbitraryData,
        screenConfig = requestBody.screenConfig ?: ScreenConfig.EMPTY
    )
}
