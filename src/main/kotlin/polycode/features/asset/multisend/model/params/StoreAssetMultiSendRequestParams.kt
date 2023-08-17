package polycode.features.asset.multisend.model.params

import com.fasterxml.jackson.databind.JsonNode
import polycode.features.api.access.model.result.Project
import polycode.generated.jooq.id.AssetMultiSendRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.model.ScreenConfig
import polycode.model.params.ParamsFactory
import polycode.util.Balance
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.UtcDateTime
import polycode.util.WalletAddress
import java.util.UUID

data class StoreAssetMultiSendRequestParams(
    val id: AssetMultiSendRequestId,
    val projectId: ProjectId,
    val chainId: ChainId,
    val redirectUrl: String,
    val tokenAddress: ContractAddress?,
    val disperseContractAddress: ContractAddress,
    val assetAmounts: List<Balance>,
    val assetRecipientAddresses: List<WalletAddress>,
    val itemNames: List<String?>,
    val assetSenderAddress: WalletAddress?,
    val arbitraryData: JsonNode?,
    val approveScreenConfig: ScreenConfig,
    val disperseScreenConfig: ScreenConfig,
    val createdAt: UtcDateTime
) {
    companion object : ParamsFactory<CreateAssetMultiSendRequestParams, StoreAssetMultiSendRequestParams> {
        private const val PATH = "/request-multi-send/\${id}/action"

        override fun fromCreateParams(
            id: UUID,
            params: CreateAssetMultiSendRequestParams,
            project: Project,
            createdAt: UtcDateTime
        ) = StoreAssetMultiSendRequestParams(
            id = AssetMultiSendRequestId(id),
            projectId = project.id,
            chainId = project.chainId,
            redirectUrl = project.createRedirectUrl(params.redirectUrl, id, PATH),
            tokenAddress = params.tokenAddress,
            disperseContractAddress = params.disperseContractAddress,
            assetAmounts = params.assetAmounts,
            assetRecipientAddresses = params.assetRecipientAddresses,
            itemNames = params.itemNames,
            assetSenderAddress = params.assetSenderAddress,
            arbitraryData = params.arbitraryData,
            disperseScreenConfig = params.disperseScreenConfig,
            approveScreenConfig = params.approveScreenConfig,
            createdAt = createdAt
        )
    }
}
