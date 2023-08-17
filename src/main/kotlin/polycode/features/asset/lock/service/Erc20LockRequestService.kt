package polycode.features.asset.lock.service

import polycode.features.api.access.model.result.Project
import polycode.features.asset.lock.model.params.CreateErc20LockRequestParams
import polycode.features.asset.lock.model.result.Erc20LockRequest
import polycode.generated.jooq.id.Erc20LockRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.util.TransactionHash
import polycode.util.WalletAddress
import polycode.util.WithFunctionData
import polycode.util.WithTransactionData

interface Erc20LockRequestService {
    fun createErc20LockRequest(
        params: CreateErc20LockRequestParams,
        project: Project
    ): WithFunctionData<Erc20LockRequest>

    fun getErc20LockRequest(id: Erc20LockRequestId): WithTransactionData<Erc20LockRequest>
    fun getErc20LockRequestsByProjectId(projectId: ProjectId): List<WithTransactionData<Erc20LockRequest>>
    fun attachTxInfo(id: Erc20LockRequestId, txHash: TransactionHash, caller: WalletAddress)
}
