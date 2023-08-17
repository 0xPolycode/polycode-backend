package polycode.features.payout.service

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import org.springframework.beans.factory.DisposableBean
import org.springframework.stereotype.Service
import polycode.blockchain.BlockchainService
import polycode.blockchain.properties.ChainSpec
import polycode.config.PayoutQueueProperties
import polycode.features.api.access.repository.ProjectRepository
import polycode.features.payout.model.params.CreateAssetSnapshotParams
import polycode.features.payout.model.params.FetchMerkleTreeParams
import polycode.features.payout.model.result.AssetSnapshot
import polycode.features.payout.model.result.FullAssetSnapshot
import polycode.features.payout.model.result.FullAssetSnapshotData
import polycode.features.payout.model.result.OptionalAssetSnapshotData
import polycode.features.payout.model.result.PendingAssetSnapshot
import polycode.features.payout.model.result.SuccessfulAssetSnapshotData
import polycode.features.payout.repository.AssetSnapshotRepository
import polycode.features.payout.repository.MerkleTreeRepository
import polycode.features.payout.util.AssetSnapshotFailureCause
import polycode.features.payout.util.AssetSnapshotStatus
import polycode.features.payout.util.HashFunction
import polycode.features.payout.util.MerkleTree
import polycode.generated.jooq.id.AssetSnapshotId
import polycode.generated.jooq.id.ProjectId
import polycode.model.result.FullContractDeploymentTransactionInfo
import polycode.service.ScheduledExecutorServiceProvider
import polycode.util.Balance
import polycode.util.BlockNumber
import java.math.BigInteger
import java.util.concurrent.TimeUnit

@Service
class AssetSnapshotQueueServiceImpl(
    private val merkleTreeRepository: MerkleTreeRepository,
    private val assetSnapshotRepository: AssetSnapshotRepository,
    private val projectRepository: ProjectRepository,
    private val ipfsService: IpfsService,
    private val blockchainService: BlockchainService,
    private val objectMapper: ObjectMapper,
    payoutQueueProperties: PayoutQueueProperties,
    scheduledExecutorServiceProvider: ScheduledExecutorServiceProvider
) : AssetSnapshotQueueService, DisposableBean {

    companion object : KLogging() {
        const val QUEUE_NAME = "AssetSnapshotQueue"
    }

    private val executorService = scheduledExecutorServiceProvider.newSingleThreadScheduledExecutor(QUEUE_NAME)

    init {
        executorService.scheduleAtFixedRate(
            command = { processAssetSnapshots() },
            initialDelay = payoutQueueProperties.initialDelay,
            period = payoutQueueProperties.polling,
            unit = TimeUnit.MILLISECONDS
        )
    }

    override fun destroy() {
        logger.info { "Shutting down asset snapshot queue executor service..." }
        executorService.shutdown()
    }

    override fun submitAssetSnapshot(params: CreateAssetSnapshotParams): AssetSnapshotId {
        logger.info { "Asset snapshot request with params: $params" }
        return assetSnapshotRepository.createAssetSnapshot(params)
    }

    override fun getAssetSnapshotById(assetSnapshotId: AssetSnapshotId): FullAssetSnapshot? {
        logger.debug { "Fetching asset snapshot, assetSnapshotId: $assetSnapshotId" }
        return assetSnapshotRepository.getById(assetSnapshotId)?.toResponse()
    }

    override fun getAllAssetSnapshotsByProjectIdAndStatuses(
        projectId: ProjectId,
        statuses: Set<AssetSnapshotStatus>
    ): List<FullAssetSnapshot> {
        logger.debug { "Fetching all asset snapshots for projectId: $projectId, statuses: $statuses" }

        return assetSnapshotRepository.getAllByProjectIdAndStatuses(projectId, statuses)
            .map { it.toResponse() }
    }

    private fun AssetSnapshot.toResponse(): FullAssetSnapshot =
        FullAssetSnapshot(
            id = id,
            projectId = projectId,
            name = name,
            chainId = chainId,
            assetContractAddress = assetContractAddress,
            blockNumber = blockNumber,
            ignoredHolderAddresses = ignoredHolderAddresses,
            snapshotStatus = data.status,
            snapshotFailureCause = data.failureCause,
            data = data.createAssetSnapshotData()
        )

    private fun OptionalAssetSnapshotData.createAssetSnapshotData(): FullAssetSnapshotData? {
        return if (this is SuccessfulAssetSnapshotData) {
            val tree = merkleTreeRepository.getById(merkleTreeRootId)

            tree?.let {
                FullAssetSnapshotData(
                    totalAssetAmount = totalAssetAmount,
                    merkleRootHash = it.root.hash,
                    merkleTreeIpfsHash = merkleTreeIpfsHash,
                    merkleTreeDepth = it.root.depth,
                    hashFn = it.hashFn
                )
            }
        } else null
    }

    @Suppress("TooGenericExceptionCaught")
    private fun processAssetSnapshots() {
        assetSnapshotRepository.getPending()?.let { assetSnapshot ->
            try {
                handlePendingAssetSnapshot(assetSnapshot)
            } catch (ex: Throwable) {
                logger.error {
                    "Failed to handle pending asset snapshot, assetSnapshotId: ${assetSnapshot.id}: ${ex.message}"
                }

                val cause = when (ex.cause?.message?.contains("Log response size exceeded")) {
                    true -> AssetSnapshotFailureCause.LOG_RESPONSE_LIMIT
                    else -> AssetSnapshotFailureCause.OTHER
                }

                assetSnapshotRepository.failAssetSnapshot(assetSnapshot.id, cause)
            }
        }
    }

    private fun handlePendingAssetSnapshot(assetSnapshot: PendingAssetSnapshot) {
        val project = projectRepository.getById(assetSnapshot.projectId)!!
        val chainSpec = ChainSpec(
            chainId = assetSnapshot.chainId,
            customRpcUrl = project.customRpcUrl
        )

        val contractDeploymentTransactionInfo = blockchainService.findContractDeploymentTransaction(
            chainSpec = chainSpec,
            contractAddress = assetSnapshot.assetContractAddress,
            events = emptyList()
        )

        val contractDeploymentBlock = (contractDeploymentTransactionInfo as? FullContractDeploymentTransactionInfo)
            ?.blockNumber ?: BlockNumber(BigInteger.ZERO)

        val balances = blockchainService.fetchErc20AccountBalances(
            chainSpec = chainSpec,
            erc20ContractAddress = assetSnapshot.assetContractAddress,
            ignoredErc20Addresses = assetSnapshot.ignoredHolderAddresses,
            startBlock = contractDeploymentBlock,
            endBlock = assetSnapshot.blockNumber
        )

        val totalAssetAmount = Balance(balances.sumOf { it.balance.rawValue })

        logger.info { "Total sum of non-ignored asset balances: $totalAssetAmount" }

        val tree = MerkleTree(balances, HashFunction.KECCAK_256)
        val alreadyInsertedTree = merkleTreeRepository.fetchTree(
            FetchMerkleTreeParams(tree.root.hash, assetSnapshot.chainId, assetSnapshot.assetContractAddress)
        )

        val rootId = if (alreadyInsertedTree != null) {
            logger.debug { "Merkle tree already exists, returning tree ID" }
            alreadyInsertedTree.treeId
        } else {
            logger.debug { "Storing Merkle tree into the database" }
            merkleTreeRepository.storeTree(
                tree = tree,
                chainId = assetSnapshot.chainId,
                assetContractAddress = assetSnapshot.assetContractAddress,
                blockNumber = assetSnapshot.blockNumber
            )
        }

        val ipfsHash = ipfsService.pinJsonToIpfs(objectMapper.valueToTree(tree))

        assetSnapshotRepository.completeAssetSnapshot(assetSnapshot.id, rootId, ipfsHash, totalAssetAmount)
        logger.info { "Asset snapshot completed: ${assetSnapshot.id}" }
    }
}
