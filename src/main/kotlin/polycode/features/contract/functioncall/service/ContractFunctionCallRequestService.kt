package polycode.features.contract.functioncall.service

import polycode.features.api.access.model.result.Project
import polycode.features.contract.functioncall.model.filters.ContractFunctionCallRequestFilters
import polycode.features.contract.functioncall.model.params.CreateContractFunctionCallRequestParams
import polycode.features.contract.functioncall.model.result.ContractFunctionCallRequest
import polycode.generated.jooq.id.ContractFunctionCallRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.util.TransactionHash
import polycode.util.WalletAddress
import polycode.util.WithFunctionData
import polycode.util.WithTransactionAndFunctionData

interface ContractFunctionCallRequestService {
    fun createContractFunctionCallRequest(
        params: CreateContractFunctionCallRequestParams,
        project: Project
    ): WithFunctionData<ContractFunctionCallRequest>

    fun getContractFunctionCallRequest(
        id: ContractFunctionCallRequestId
    ): WithTransactionAndFunctionData<ContractFunctionCallRequest>

    fun getContractFunctionCallRequestsByProjectIdAndFilters(
        projectId: ProjectId,
        filters: ContractFunctionCallRequestFilters
    ): List<WithTransactionAndFunctionData<ContractFunctionCallRequest>>

    fun attachTxInfo(id: ContractFunctionCallRequestId, txHash: TransactionHash, caller: WalletAddress)
}
