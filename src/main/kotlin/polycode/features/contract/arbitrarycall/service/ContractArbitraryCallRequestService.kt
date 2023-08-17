package polycode.features.contract.arbitrarycall.service

import polycode.features.api.access.model.result.Project
import polycode.features.contract.arbitrarycall.model.filters.ContractArbitraryCallRequestFilters
import polycode.features.contract.arbitrarycall.model.params.CreateContractArbitraryCallRequestParams
import polycode.features.contract.arbitrarycall.model.result.ContractArbitraryCallRequest
import polycode.generated.jooq.id.ContractArbitraryCallRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.util.TransactionHash
import polycode.util.WalletAddress
import polycode.util.WithTransactionData

interface ContractArbitraryCallRequestService {
    fun createContractArbitraryCallRequest(
        params: CreateContractArbitraryCallRequestParams,
        project: Project
    ): ContractArbitraryCallRequest

    fun getContractArbitraryCallRequest(
        id: ContractArbitraryCallRequestId
    ): WithTransactionData<ContractArbitraryCallRequest>

    fun getContractArbitraryCallRequestsByProjectIdAndFilters(
        projectId: ProjectId,
        filters: ContractArbitraryCallRequestFilters
    ): List<WithTransactionData<ContractArbitraryCallRequest>>

    fun attachTxInfo(id: ContractArbitraryCallRequestId, txHash: TransactionHash, caller: WalletAddress)
}
