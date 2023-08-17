package polycode.features.asset.lock.repository

import polycode.features.asset.lock.model.params.StoreErc20LockRequestParams
import polycode.features.asset.lock.model.result.Erc20LockRequest
import polycode.generated.jooq.id.Erc20LockRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.util.TransactionHash
import polycode.util.WalletAddress

interface Erc20LockRequestRepository {
    fun store(params: StoreErc20LockRequestParams): Erc20LockRequest
    fun getById(id: Erc20LockRequestId): Erc20LockRequest?
    fun getAllByProjectId(projectId: ProjectId): List<Erc20LockRequest>
    fun setTxInfo(id: Erc20LockRequestId, txHash: TransactionHash, caller: WalletAddress): Boolean
}
