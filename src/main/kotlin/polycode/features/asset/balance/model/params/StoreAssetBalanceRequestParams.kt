package polycode.features.asset.balance.model.params

import com.fasterxml.jackson.databind.JsonNode
import polycode.features.api.access.model.result.Project
import polycode.generated.jooq.id.AssetBalanceRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.model.ScreenConfig
import polycode.model.params.ParamsFactory
import polycode.util.BlockNumber
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.UtcDateTime
import polycode.util.WalletAddress
import java.util.UUID

data class StoreAssetBalanceRequestParams(
    val id: AssetBalanceRequestId,
    val projectId: ProjectId,
    val chainId: ChainId,
    val redirectUrl: String,
    val tokenAddress: ContractAddress?,
    val blockNumber: BlockNumber?,
    val requestedWalletAddress: WalletAddress?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig,
    val createdAt: UtcDateTime
) {
    companion object : ParamsFactory<CreateAssetBalanceRequestParams, StoreAssetBalanceRequestParams> {
        private const val PATH = "/request-balance/\${id}/action"

        override fun fromCreateParams(
            id: UUID,
            params: CreateAssetBalanceRequestParams,
            project: Project,
            createdAt: UtcDateTime
        ) = StoreAssetBalanceRequestParams(
            id = AssetBalanceRequestId(id),
            projectId = project.id,
            chainId = project.chainId,
            redirectUrl = project.createRedirectUrl(params.redirectUrl, id, PATH),
            tokenAddress = params.tokenAddress,
            blockNumber = params.blockNumber,
            requestedWalletAddress = params.requestedWalletAddress,
            arbitraryData = params.arbitraryData,
            screenConfig = params.screenConfig,
            createdAt = createdAt
        )
    }
}
