package polycode.features.contract.arbitrarycall.repository

import polycode.features.contract.arbitrarycall.model.filters.ContractArbitraryCallRequestFilters
import polycode.features.contract.arbitrarycall.model.params.StoreContractArbitraryCallRequestParams
import polycode.features.contract.arbitrarycall.model.result.ContractArbitraryCallRequest
import polycode.generated.jooq.id.ContractArbitraryCallRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.util.TransactionHash
import polycode.util.WalletAddress

interface ContractArbitraryCallRequestRepository {
    fun store(params: StoreContractArbitraryCallRequestParams): ContractArbitraryCallRequest
    fun getById(id: ContractArbitraryCallRequestId): ContractArbitraryCallRequest?
    fun getAllByProjectId(
        projectId: ProjectId,
        filters: ContractArbitraryCallRequestFilters
    ): List<ContractArbitraryCallRequest>

    fun setTxInfo(id: ContractArbitraryCallRequestId, txHash: TransactionHash, caller: WalletAddress): Boolean
}
