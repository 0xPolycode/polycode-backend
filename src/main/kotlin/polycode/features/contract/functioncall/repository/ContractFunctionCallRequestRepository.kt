package polycode.features.contract.functioncall.repository

import polycode.features.contract.functioncall.model.filters.ContractFunctionCallRequestFilters
import polycode.features.contract.functioncall.model.params.StoreContractFunctionCallRequestParams
import polycode.features.contract.functioncall.model.result.ContractFunctionCallRequest
import polycode.generated.jooq.id.ContractFunctionCallRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.util.TransactionHash
import polycode.util.WalletAddress

interface ContractFunctionCallRequestRepository {
    fun store(params: StoreContractFunctionCallRequestParams): ContractFunctionCallRequest
    fun getById(id: ContractFunctionCallRequestId): ContractFunctionCallRequest?
    fun getAllByProjectId(
        projectId: ProjectId,
        filters: ContractFunctionCallRequestFilters
    ): List<ContractFunctionCallRequest>

    fun setTxInfo(id: ContractFunctionCallRequestId, txHash: TransactionHash, caller: WalletAddress): Boolean
}
