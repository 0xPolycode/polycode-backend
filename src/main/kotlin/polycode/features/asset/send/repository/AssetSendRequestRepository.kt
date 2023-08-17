package polycode.features.asset.send.repository

import polycode.features.asset.send.model.params.StoreAssetSendRequestParams
import polycode.features.asset.send.model.result.AssetSendRequest
import polycode.generated.jooq.id.AssetSendRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.util.TransactionHash
import polycode.util.WalletAddress

interface AssetSendRequestRepository {
    fun store(params: StoreAssetSendRequestParams): AssetSendRequest
    fun getById(id: AssetSendRequestId): AssetSendRequest?
    fun getAllByProjectId(projectId: ProjectId): List<AssetSendRequest>
    fun getBySender(sender: WalletAddress): List<AssetSendRequest>
    fun getByRecipient(recipient: WalletAddress): List<AssetSendRequest>
    fun setTxInfo(id: AssetSendRequestId, txHash: TransactionHash, caller: WalletAddress): Boolean
}
