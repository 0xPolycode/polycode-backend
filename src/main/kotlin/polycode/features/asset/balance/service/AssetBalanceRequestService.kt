package polycode.features.asset.balance.service

import polycode.features.api.access.model.result.Project
import polycode.features.asset.balance.model.params.CreateAssetBalanceRequestParams
import polycode.features.asset.balance.model.result.AssetBalanceRequest
import polycode.features.asset.balance.model.result.FullAssetBalanceRequest
import polycode.generated.jooq.id.AssetBalanceRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.util.SignedMessage
import polycode.util.WalletAddress

interface AssetBalanceRequestService {
    fun createAssetBalanceRequest(params: CreateAssetBalanceRequestParams, project: Project): AssetBalanceRequest
    fun getAssetBalanceRequest(id: AssetBalanceRequestId): FullAssetBalanceRequest
    fun getAssetBalanceRequestsByProjectId(projectId: ProjectId): List<FullAssetBalanceRequest>
    fun attachWalletAddressAndSignedMessage(
        id: AssetBalanceRequestId,
        walletAddress: WalletAddress,
        signedMessage: SignedMessage
    )
}
