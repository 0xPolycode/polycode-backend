package polycode.features.asset.multisend.service

import polycode.features.api.access.model.result.Project
import polycode.features.asset.multisend.model.params.CreateAssetMultiSendRequestParams
import polycode.features.asset.multisend.model.result.AssetMultiSendRequest
import polycode.generated.jooq.id.AssetMultiSendRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.util.TransactionHash
import polycode.util.WalletAddress
import polycode.util.WithFunctionDataOrEthValue
import polycode.util.WithMultiTransactionData

interface AssetMultiSendRequestService {
    fun createAssetMultiSendRequest(
        params: CreateAssetMultiSendRequestParams,
        project: Project
    ): WithFunctionDataOrEthValue<AssetMultiSendRequest>

    fun getAssetMultiSendRequest(id: AssetMultiSendRequestId): WithMultiTransactionData<AssetMultiSendRequest>
    fun getAssetMultiSendRequestsByProjectId(
        projectId: ProjectId
    ): List<WithMultiTransactionData<AssetMultiSendRequest>>

    fun getAssetMultiSendRequestsBySender(sender: WalletAddress): List<WithMultiTransactionData<AssetMultiSendRequest>>
    fun attachApproveTxInfo(id: AssetMultiSendRequestId, txHash: TransactionHash, caller: WalletAddress)
    fun attachDisperseTxInfo(id: AssetMultiSendRequestId, txHash: TransactionHash, caller: WalletAddress)
}
