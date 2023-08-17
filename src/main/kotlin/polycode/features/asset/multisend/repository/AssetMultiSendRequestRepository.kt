package polycode.features.asset.multisend.repository

import polycode.features.asset.multisend.model.params.StoreAssetMultiSendRequestParams
import polycode.features.asset.multisend.model.result.AssetMultiSendRequest
import polycode.generated.jooq.id.AssetMultiSendRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.util.TransactionHash
import polycode.util.WalletAddress

interface AssetMultiSendRequestRepository {
    fun store(params: StoreAssetMultiSendRequestParams): AssetMultiSendRequest
    fun getById(id: AssetMultiSendRequestId): AssetMultiSendRequest?
    fun getAllByProjectId(projectId: ProjectId): List<AssetMultiSendRequest>
    fun getBySender(sender: WalletAddress): List<AssetMultiSendRequest>
    fun setApproveTxInfo(id: AssetMultiSendRequestId, txHash: TransactionHash, caller: WalletAddress): Boolean
    fun setDisperseTxInfo(id: AssetMultiSendRequestId, txHash: TransactionHash, caller: WalletAddress): Boolean
}
