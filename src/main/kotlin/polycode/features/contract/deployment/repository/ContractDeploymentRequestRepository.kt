package polycode.features.contract.deployment.repository

import polycode.features.contract.deployment.model.filters.ContractDeploymentRequestFilters
import polycode.features.contract.deployment.model.params.StoreContractDeploymentRequestParams
import polycode.features.contract.deployment.model.result.ContractDeploymentRequest
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.TransactionHash
import polycode.util.WalletAddress

interface ContractDeploymentRequestRepository {
    fun store(params: StoreContractDeploymentRequestParams, metadataProjectId: ProjectId): ContractDeploymentRequest
    fun markAsDeleted(id: ContractDeploymentRequestId): Boolean
    fun getById(id: ContractDeploymentRequestId): ContractDeploymentRequest?
    fun getByAliasAndProjectId(alias: String, projectId: ProjectId): ContractDeploymentRequest?
    fun getByContractAddressAndChainId(contractAddress: ContractAddress, chainId: ChainId): ContractDeploymentRequest?
    fun getByContractAddressChainIdAndProjectId(
        contractAddress: ContractAddress,
        chainId: ChainId,
        projectId: ProjectId
    ): ContractDeploymentRequest?

    fun getAllByProjectId(
        projectId: ProjectId,
        filters: ContractDeploymentRequestFilters
    ): List<ContractDeploymentRequest>

    fun setTxInfo(id: ContractDeploymentRequestId, txHash: TransactionHash, deployer: WalletAddress): Boolean
    fun setContractAddress(id: ContractDeploymentRequestId, contractAddress: ContractAddress): Boolean
}
