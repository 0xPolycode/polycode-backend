package polycode.features.asset.lock.model.params

import com.fasterxml.jackson.databind.JsonNode
import polycode.features.api.access.model.result.Project
import polycode.generated.jooq.id.Erc20LockRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.model.ScreenConfig
import polycode.model.params.ParamsFactory
import polycode.util.Balance
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.DurationSeconds
import polycode.util.UtcDateTime
import polycode.util.WalletAddress
import java.util.UUID

data class StoreErc20LockRequestParams(
    val id: Erc20LockRequestId,
    val projectId: ProjectId,
    val chainId: ChainId,
    val redirectUrl: String,
    val tokenAddress: ContractAddress,
    val tokenAmount: Balance,
    val lockDuration: DurationSeconds,
    val lockContractAddress: ContractAddress,
    val tokenSenderAddress: WalletAddress?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig,
    val createdAt: UtcDateTime
) {
    companion object : ParamsFactory<CreateErc20LockRequestParams, StoreErc20LockRequestParams> {
        private const val PATH = "/request-lock/\${id}/action"

        override fun fromCreateParams(
            id: UUID,
            params: CreateErc20LockRequestParams,
            project: Project,
            createdAt: UtcDateTime
        ) = StoreErc20LockRequestParams(
            id = Erc20LockRequestId(id),
            projectId = project.id,
            chainId = project.chainId,
            redirectUrl = project.createRedirectUrl(params.redirectUrl, id, PATH),
            tokenAddress = params.tokenAddress,
            tokenAmount = params.tokenAmount,
            lockDuration = params.lockDuration,
            lockContractAddress = params.lockContractAddress,
            tokenSenderAddress = params.tokenSenderAddress,
            arbitraryData = params.arbitraryData,
            screenConfig = params.screenConfig,
            createdAt = createdAt
        )
    }
}
