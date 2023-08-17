package polycode.features.asset.send.service

import polycode.features.api.access.model.result.Project
import polycode.features.asset.send.model.params.CreateAssetSendRequestParams
import polycode.features.asset.send.model.result.AssetSendRequest
import polycode.generated.jooq.id.AssetSendRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.util.TransactionHash
import polycode.util.WalletAddress
import polycode.util.WithFunctionDataOrEthValue
import polycode.util.WithTransactionData

interface AssetSendRequestService {
    fun createAssetSendRequest(
        params: CreateAssetSendRequestParams,
        project: Project
    ): WithFunctionDataOrEthValue<AssetSendRequest>

    fun getAssetSendRequest(id: AssetSendRequestId): WithTransactionData<AssetSendRequest>
    fun getAssetSendRequestsByProjectId(projectId: ProjectId): List<WithTransactionData<AssetSendRequest>>
    fun getAssetSendRequestsBySender(sender: WalletAddress): List<WithTransactionData<AssetSendRequest>>
    fun getAssetSendRequestsByRecipient(recipient: WalletAddress): List<WithTransactionData<AssetSendRequest>>
    fun attachTxInfo(id: AssetSendRequestId, txHash: TransactionHash, caller: WalletAddress)
}
