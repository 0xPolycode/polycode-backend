package polycode.features.asset.balance.repository

import polycode.features.asset.balance.model.params.StoreAssetBalanceRequestParams
import polycode.features.asset.balance.model.result.AssetBalanceRequest
import polycode.generated.jooq.id.AssetBalanceRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.util.SignedMessage
import polycode.util.WalletAddress

interface AssetBalanceRequestRepository {
    fun store(params: StoreAssetBalanceRequestParams): AssetBalanceRequest
    fun getById(id: AssetBalanceRequestId): AssetBalanceRequest?
    fun getAllByProjectId(projectId: ProjectId): List<AssetBalanceRequest>
    fun setSignedMessage(id: AssetBalanceRequestId, walletAddress: WalletAddress, signedMessage: SignedMessage): Boolean
}
