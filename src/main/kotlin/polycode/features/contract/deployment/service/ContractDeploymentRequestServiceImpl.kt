package polycode.features.contract.deployment.service

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import org.springframework.stereotype.Service
import polycode.exception.CannotAttachTxInfoException
import polycode.exception.ResourceNotFoundException
import polycode.features.api.access.model.result.Project
import polycode.features.api.access.repository.ProjectRepository
import polycode.features.contract.deployment.model.filters.ContractDeploymentRequestFilters
import polycode.features.contract.deployment.model.params.CreateContractDeploymentRequestParams
import polycode.features.contract.deployment.model.params.PreStoreContractDeploymentRequestParams
import polycode.features.contract.deployment.model.params.StoreContractDeploymentRequestParams
import polycode.features.contract.deployment.model.result.ContractDeploymentRequest
import polycode.features.contract.deployment.repository.ContractDecoratorRepository
import polycode.features.contract.deployment.repository.ContractDeploymentRequestRepository
import polycode.features.contract.deployment.repository.ContractMetadataRepository
import polycode.features.contract.deployment.repository.ImportedContractDecoratorRepository
import polycode.features.functions.encoding.service.FunctionEncoderService
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.model.result.BlockchainTransactionInfo
import polycode.service.EthCommonService
import polycode.util.Constants
import polycode.util.ContractAddress
import polycode.util.FunctionData
import polycode.util.Status
import polycode.util.TransactionHash
import polycode.util.WalletAddress
import polycode.util.WithTransactionData
import polycode.util.ZeroAddress

@Service
@Suppress("TooManyFunctions")
class ContractDeploymentRequestServiceImpl(
    private val functionEncoderService: FunctionEncoderService,
    private val contractDeploymentRequestRepository: ContractDeploymentRequestRepository,
    private val contractMetadataRepository: ContractMetadataRepository,
    private val contractDecoratorRepository: ContractDecoratorRepository,
    private val importedContractDecoratorRepository: ImportedContractDecoratorRepository,
    private val ethCommonService: EthCommonService,
    private val projectRepository: ProjectRepository,
    private val objectMapper: ObjectMapper
) : ContractDeploymentRequestService {

    companion object : KLogging()

    override fun createContractDeploymentRequest(
        params: CreateContractDeploymentRequestParams,
        project: Project
    ): ContractDeploymentRequest {
        logger.info { "Creating contract deployment request, params: $params, project: $project" }

        val decoratorNotFoundMessage = "Contract decorator not found for contract ID: ${params.contractId.value}"

        val contractDecorator = ethCommonService.fetchResource(
            contractDecoratorRepository.getById(params.contractId),
            decoratorNotFoundMessage
        )

        if (!contractMetadataRepository.exists(params.contractId, Constants.NIL_PROJECT_ID)) {
            throw ResourceNotFoundException(decoratorNotFoundMessage)
        }

        // TODO check if constructor exists (out of MVP scope)
        val encodedConstructor = functionEncoderService.encodeConstructor(params.constructorParams)
        val preStoreParams = PreStoreContractDeploymentRequestParams(
            createParams = params,
            contractDecorator = contractDecorator,
            encodedConstructor = encodedConstructor
        )
        val databaseParams = ethCommonService.createDatabaseParams(
            factory = StoreContractDeploymentRequestParams,
            params = preStoreParams,
            project = project
        )

        return contractDeploymentRequestRepository.store(databaseParams, Constants.NIL_PROJECT_ID)
    }

    override fun markContractDeploymentRequestAsDeleted(id: ContractDeploymentRequestId, projectId: ProjectId) {
        logger.info { "Mark contract deployment request as deleted by id: $id, projectId: $projectId" }
        ethCommonService.fetchResource(
            contractDeploymentRequestRepository.getById(id)?.takeIf { it.projectId == projectId },
            "Contract deployment request not found for ID: $id"
        ).let { contractDeploymentRequestRepository.markAsDeleted(it.id) }
    }

    override fun getContractDeploymentRequest(
        id: ContractDeploymentRequestId
    ): WithTransactionData<ContractDeploymentRequest> {
        logger.debug { "Fetching contract deployment request, id: $id" }

        val contractDeploymentRequest = ethCommonService.fetchResource(
            contractDeploymentRequestRepository.getById(id),
            "Contract deployment request not found for ID: $id"
        )
        val project = projectRepository.getById(contractDeploymentRequest.projectId)!!

        return contractDeploymentRequest.appendTransactionData(project)
    }

    override fun getContractDeploymentRequestsByProjectIdAndFilters(
        projectId: ProjectId,
        filters: ContractDeploymentRequestFilters
    ): List<WithTransactionData<ContractDeploymentRequest>> {
        logger.debug { "Fetching contract deployment requests for projectId: $projectId, filters: $filters" }

        val requests = projectRepository.getById(projectId)?.let {
            contractDeploymentRequestRepository.getAllByProjectId(projectId, filters)
                .map { req -> req.appendTransactionData(it) }
        } ?: emptyList()

        return if (filters.deployedOnly) {
            requests.filter { it.status == Status.SUCCESS }
        } else {
            requests
        }
    }

    override fun getContractDeploymentRequestByProjectIdAndAlias(
        projectId: ProjectId,
        alias: String
    ): WithTransactionData<ContractDeploymentRequest> {
        logger.debug { "Fetching contract deployment requests for projectId: $projectId, alias: $alias" }

        val contractDeploymentRequest = ethCommonService.fetchResource(
            contractDeploymentRequestRepository.getByAliasAndProjectId(alias, projectId),
            "Contract deployment request not found for projectId: $projectId and alias: $alias"
        )
        val project = projectRepository.getById(contractDeploymentRequest.projectId)!!

        return contractDeploymentRequest.appendTransactionData(project)
    }

    override fun attachTxInfo(id: ContractDeploymentRequestId, txHash: TransactionHash, deployer: WalletAddress) {
        logger.info { "Attach txInfo to contract deployment request, id: $id, txHash: $txHash, deployer: $deployer" }

        val txInfoAttached = contractDeploymentRequestRepository.setTxInfo(id, txHash, deployer)

        if (txInfoAttached.not()) {
            throw CannotAttachTxInfoException(
                "Unable to attach transaction info to contract deployment request with ID: $id"
            )
        }
    }

    private fun ContractDeploymentRequest.appendTransactionData(
        project: Project
    ): WithTransactionData<ContractDeploymentRequest> {
        val decorator = contractDecoratorRepository.getById(contractId)
            ?: importedContractDecoratorRepository.getByContractIdAndProjectId(contractId, projectId)
            ?: throw ResourceNotFoundException(
                "Contract decorator not found for contract ID: $contractId, project ID: $projectId"
            )
        val transactionInfo = ethCommonService.fetchTransactionInfo(
            txHash = txHash,
            chainId = chainId,
            customRpcUrl = project.customRpcUrl,
            events = decorator.getDeserializableEvents(objectMapper)
        )

        val request = setContractAddressIfNecessary(transactionInfo?.deployedContractAddress)
        val status = request.determineStatus(transactionInfo)

        return request.withTransactionData(
            status = status,
            transactionInfo = transactionInfo
        )
    }

    private fun ContractDeploymentRequest.setContractAddressIfNecessary(
        deployedContractAddress: ContractAddress?
    ): ContractDeploymentRequest { // TODO update proxy address if it changes
        return if (contractAddress == null && deployedContractAddress != null) {
            contractDeploymentRequestRepository.setContractAddress(id, deployedContractAddress)
            copy(contractAddress = deployedContractAddress)
        } else {
            this
        }
    }

    private fun ContractDeploymentRequest.determineStatus(
        transactionInfo: BlockchainTransactionInfo?
    ): Status =
        if (imported) {
            Status.SUCCESS
        } else if (transactionInfo == null) { // implies that either txHash is null or transaction is not yet mined
            Status.PENDING
        } else if (isSuccess(transactionInfo)) {
            Status.SUCCESS
        } else {
            Status.FAILED
        }

    private fun ContractDeploymentRequest.isSuccess(
        transactionInfo: BlockchainTransactionInfo
    ): Boolean =
        transactionInfo.success &&
            transactionInfo.hashMatches(txHash) &&
            transactionInfo.fromAddressOptionallyMatches(deployerAddress) &&
            transactionInfo.toAddressMatches(ZeroAddress) &&
            transactionInfo.deployedContractAddressMatches(contractAddress) &&
            transactionInfo.dataMatches(FunctionData(contractData.value)) &&
            transactionInfo.valueMatches(initialEthAmount)
}
