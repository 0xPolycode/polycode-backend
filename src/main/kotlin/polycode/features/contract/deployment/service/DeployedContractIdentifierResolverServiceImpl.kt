package polycode.features.contract.deployment.service

import mu.KLogging
import org.springframework.stereotype.Service
import polycode.exception.ContractNotYetDeployedException
import polycode.exception.ResourceNotFoundException
import polycode.features.api.access.model.result.Project
import polycode.features.contract.deployment.model.params.DeployedContractAddressIdentifier
import polycode.features.contract.deployment.model.params.DeployedContractAliasIdentifier
import polycode.features.contract.deployment.model.params.DeployedContractIdIdentifier
import polycode.features.contract.deployment.model.params.DeployedContractIdentifier
import polycode.features.contract.deployment.model.result.ContractDeploymentRequest
import polycode.features.contract.deployment.repository.ContractDeploymentRequestRepository
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.service.EthCommonService
import polycode.util.ContractAddress

private typealias IdAndAddress = Pair<ContractDeploymentRequestId?, ContractAddress>

@Service
class DeployedContractIdentifierResolverServiceImpl(
    private val contractDeploymentRequestRepository: ContractDeploymentRequestRepository,
    private val ethCommonService: EthCommonService
) : DeployedContractIdentifierResolverService {

    companion object : KLogging()

    override fun resolveContractIdAndAddress(
        identifier: DeployedContractIdentifier,
        project: Project
    ): Pair<ContractDeploymentRequestId?, ContractAddress> =
        when (identifier) {
            is DeployedContractIdIdentifier -> {
                logger.info { "Fetching deployed contract by id: ${identifier.id}" }
                contractDeploymentRequestRepository.getById(identifier.id)
                    ?.setContractAddressIfNecessary(project)
                    ?.deployedContractIdAndAddress()
                    ?: throw ResourceNotFoundException("Deployed contract not found for ID: ${identifier.id}")
            }

            is DeployedContractAliasIdentifier -> {
                logger.info { "Fetching deployed contract by id: ${identifier.alias}, projectId: ${project.id}" }
                contractDeploymentRequestRepository.getByAliasAndProjectId(identifier.alias, project.id)
                    ?.setContractAddressIfNecessary(project)
                    ?.deployedContractIdAndAddress()
                    ?: throw ResourceNotFoundException("Deployed contract not found for alias: ${identifier.alias}")
            }

            is DeployedContractAddressIdentifier -> {
                logger.info { "Using contract address for function call: ${identifier.contractAddress}" }
                val deploymentRequest = contractDeploymentRequestRepository.getByContractAddressChainIdAndProjectId(
                    contractAddress = identifier.contractAddress,
                    chainId = project.chainId,
                    projectId = project.id
                )
                Pair(deploymentRequest?.id, identifier.contractAddress)
            }
        }

    private fun ContractDeploymentRequest.deployedContractIdAndAddress(): IdAndAddress =
        Pair(id, contractAddress ?: throw ContractNotYetDeployedException(id, alias))

    private fun ContractDeploymentRequest.setContractAddressIfNecessary(project: Project): ContractDeploymentRequest =
        if (contractAddress == null) {
            ethCommonService.fetchTransactionInfo(
                txHash = txHash,
                chainId = chainId,
                customRpcUrl = project.customRpcUrl,
                events = emptyList()
            )?.deployedContractAddress?.let {
                contractDeploymentRequestRepository.setContractAddress(id, it)
                copy(contractAddress = it)
            } ?: this
        } else {
            this
        }
}
