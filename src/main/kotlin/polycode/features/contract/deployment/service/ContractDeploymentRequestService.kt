package polycode.features.contract.deployment.service

import polycode.features.api.access.model.result.Project
import polycode.features.contract.deployment.model.filters.ContractDeploymentRequestFilters
import polycode.features.contract.deployment.model.params.CreateContractDeploymentRequestParams
import polycode.features.contract.deployment.model.result.ContractDeploymentRequest
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.util.TransactionHash
import polycode.util.WalletAddress
import polycode.util.WithTransactionData

interface ContractDeploymentRequestService {
    fun createContractDeploymentRequest(
        params: CreateContractDeploymentRequestParams,
        project: Project
    ): ContractDeploymentRequest

    fun markContractDeploymentRequestAsDeleted(id: ContractDeploymentRequestId, projectId: ProjectId)

    fun getContractDeploymentRequest(id: ContractDeploymentRequestId): WithTransactionData<ContractDeploymentRequest>
    fun getContractDeploymentRequestsByProjectIdAndFilters(
        projectId: ProjectId,
        filters: ContractDeploymentRequestFilters
    ): List<WithTransactionData<ContractDeploymentRequest>>

    fun getContractDeploymentRequestByProjectIdAndAlias(
        projectId: ProjectId,
        alias: String
    ): WithTransactionData<ContractDeploymentRequest>

    fun attachTxInfo(id: ContractDeploymentRequestId, txHash: TransactionHash, deployer: WalletAddress)
}
