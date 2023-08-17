package polycode.features.asset.send.model.params

import com.fasterxml.jackson.databind.JsonNode
import polycode.features.api.access.model.result.Project
import polycode.generated.jooq.id.AssetSendRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.model.ScreenConfig
import polycode.model.params.ParamsFactory
import polycode.util.Balance
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.UtcDateTime
import polycode.util.WalletAddress
import java.util.UUID

data class StoreAssetSendRequestParams(
    val id: AssetSendRequestId,
    val projectId: ProjectId,
    val chainId: ChainId,
    val redirectUrl: String,
    val tokenAddress: ContractAddress?,
    val assetAmount: Balance,
    val assetSenderAddress: WalletAddress?,
    val assetRecipientAddress: WalletAddress,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig,
    val createdAt: UtcDateTime
) {
    companion object : ParamsFactory<CreateAssetSendRequestParams, StoreAssetSendRequestParams> {
        private const val PATH = "/request-send/\${id}/action"

        override fun fromCreateParams(
            id: UUID,
            params: CreateAssetSendRequestParams,
            project: Project,
            createdAt: UtcDateTime
        ) = StoreAssetSendRequestParams(
            id = AssetSendRequestId(id),
            projectId = project.id,
            chainId = project.chainId,
            redirectUrl = project.createRedirectUrl(params.redirectUrl, id, PATH),
            tokenAddress = params.tokenAddress,
            assetAmount = params.assetAmount,
            assetSenderAddress = params.assetSenderAddress,
            assetRecipientAddress = params.assetRecipientAddress,
            arbitraryData = params.arbitraryData,
            screenConfig = params.screenConfig,
            createdAt = createdAt
        )
    }
}
