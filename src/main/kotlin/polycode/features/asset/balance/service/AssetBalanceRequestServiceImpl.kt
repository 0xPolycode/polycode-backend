package polycode.features.asset.balance.service

import mu.KLogging
import org.springframework.stereotype.Service
import polycode.blockchain.BlockchainService
import polycode.blockchain.properties.ChainSpec
import polycode.exception.CannotAttachSignedMessageException
import polycode.features.api.access.model.result.Project
import polycode.features.api.access.repository.ProjectRepository
import polycode.features.asset.balance.model.params.CreateAssetBalanceRequestParams
import polycode.features.asset.balance.model.params.StoreAssetBalanceRequestParams
import polycode.features.asset.balance.model.result.AssetBalanceRequest
import polycode.features.asset.balance.model.result.FullAssetBalanceRequest
import polycode.features.asset.balance.repository.AssetBalanceRequestRepository
import polycode.features.wallet.authorization.service.SignatureCheckerService
import polycode.generated.jooq.id.AssetBalanceRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.service.EthCommonService
import polycode.util.AccountBalance
import polycode.util.BlockName
import polycode.util.SignedMessage
import polycode.util.Status
import polycode.util.WalletAddress

@Service
class AssetBalanceRequestServiceImpl(
    private val signatureCheckerService: SignatureCheckerService,
    private val blockchainService: BlockchainService,
    private val assetBalanceRequestRepository: AssetBalanceRequestRepository,
    private val ethCommonService: EthCommonService,
    private val projectRepository: ProjectRepository
) : AssetBalanceRequestService {

    companion object : KLogging()

    override fun createAssetBalanceRequest(
        params: CreateAssetBalanceRequestParams,
        project: Project
    ): AssetBalanceRequest {
        logger.info { "Creating asset balance request, params: $params, project: $project" }
        return assetBalanceRequestRepository.store(
            ethCommonService.createDatabaseParams(StoreAssetBalanceRequestParams, params, project)
        )
    }

    override fun getAssetBalanceRequest(id: AssetBalanceRequestId): FullAssetBalanceRequest {
        logger.debug { "Fetching asset balance request, id: $id" }

        val assetBalanceRequest = ethCommonService.fetchResource(
            assetBalanceRequestRepository.getById(id),
            "Asset balance check request not found for ID: $id"
        )
        val project = projectRepository.getById(assetBalanceRequest.projectId)!!

        return assetBalanceRequest.appendBalanceData(project)
    }

    override fun getAssetBalanceRequestsByProjectId(projectId: ProjectId): List<FullAssetBalanceRequest> {
        logger.debug { "Fetching asset balance requests for projectId: $projectId" }
        return projectRepository.getById(projectId)?.let {
            assetBalanceRequestRepository.getAllByProjectId(projectId).map { req -> req.appendBalanceData(it) }
        } ?: emptyList()
    }

    override fun attachWalletAddressAndSignedMessage(
        id: AssetBalanceRequestId,
        walletAddress: WalletAddress,
        signedMessage: SignedMessage
    ) {
        logger.info {
            "Attach walletAddress and signedMessage to asset balance request, id: $id, walletAddress: $walletAddress," +
                " signedMessage: $signedMessage"
        }

        val attached = assetBalanceRequestRepository.setSignedMessage(id, walletAddress, signedMessage)

        if (attached.not()) {
            throw CannotAttachSignedMessageException(
                "Unable to attach signed message to asset balance request with ID: ${id.value}"
            )
        }
    }

    private fun AssetBalanceRequest.appendBalanceData(project: Project): FullAssetBalanceRequest {
        val balance = actualWalletAddress?.let { fetchBalance(it, project) }
        val status = determineStatus(balance)

        return FullAssetBalanceRequest.fromAssetBalanceRequest(
            request = this,
            status = status,
            balance = balance
        )
    }

    private fun AssetBalanceRequest.fetchBalance(walletAddress: WalletAddress, project: Project): AccountBalance {
        val chainSpec = ChainSpec(
            chainId = chainId,
            customRpcUrl = project.customRpcUrl
        )

        return if (tokenAddress != null) {
            blockchainService.fetchErc20AccountBalance(
                chainSpec = chainSpec,
                contractAddress = tokenAddress,
                walletAddress = walletAddress,
                blockParameter = blockNumber ?: BlockName.LATEST
            )
        } else {
            blockchainService.fetchAccountBalance(
                chainSpec = chainSpec,
                walletAddress = walletAddress,
                blockParameter = blockNumber ?: BlockName.LATEST
            )
        }
    }

    private fun AssetBalanceRequest.determineStatus(balance: AccountBalance?): Status =
        if (balance == null || this.signedMessage == null) {
            Status.PENDING
        } else if (isSuccess()) {
            Status.SUCCESS
        } else {
            Status.FAILED
        }

    private fun AssetBalanceRequest.isSuccess(): Boolean {
        return walletAddressMatches(this.requestedWalletAddress, this.actualWalletAddress) &&
            signedMessage != null && actualWalletAddress != null &&
            signatureCheckerService.signatureMatches(messageToSign, signedMessage, actualWalletAddress)
    }

    private fun walletAddressMatches(requestedAddress: WalletAddress?, actualAddress: WalletAddress?): Boolean =
        requestedAddress == null || requestedAddress == actualAddress
}
