package polycode.features.payout.service

import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import polycode.TestBase
import polycode.TestData
import polycode.blockchain.BlockchainService
import polycode.blockchain.properties.ChainSpec
import polycode.config.JsonConfig
import polycode.config.PayoutQueueProperties
import polycode.features.api.access.model.result.Project
import polycode.features.api.access.repository.ProjectRepository
import polycode.features.payout.model.params.CreateAssetSnapshotParams
import polycode.features.payout.model.params.FetchMerkleTreeParams
import polycode.features.payout.model.result.AssetSnapshot
import polycode.features.payout.model.result.FullAssetSnapshot
import polycode.features.payout.model.result.FullAssetSnapshotData
import polycode.features.payout.model.result.MerkleTreeWithId
import polycode.features.payout.model.result.OtherAssetSnapshotData
import polycode.features.payout.model.result.PendingAssetSnapshot
import polycode.features.payout.model.result.SuccessfulAssetSnapshotData
import polycode.features.payout.repository.AssetSnapshotRepository
import polycode.features.payout.repository.MerkleTreeRepository
import polycode.features.payout.util.AssetSnapshotFailureCause
import polycode.features.payout.util.AssetSnapshotStatus
import polycode.features.payout.util.HashFunction
import polycode.features.payout.util.IpfsHash
import polycode.features.payout.util.MerkleTree
import polycode.features.payout.util.PayoutAccountBalance
import polycode.generated.jooq.id.AssetSnapshotId
import polycode.generated.jooq.id.MerkleTreeRootId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import polycode.model.result.FullContractDeploymentTransactionInfo
import polycode.service.ScheduledExecutorServiceProvider
import polycode.util.Balance
import polycode.util.BaseUrl
import polycode.util.BlockNumber
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.ContractBinaryData
import polycode.util.FunctionData
import polycode.util.TransactionHash
import polycode.util.WalletAddress
import polycode.util.ZeroAddress
import java.math.BigInteger
import java.util.UUID

class AssetSnapshotQueueServiceTest : TestBase() {

    companion object {
        private val PROJECT = Project(
            id = ProjectId(UUID.randomUUID()),
            ownerId = UserId(UUID.randomUUID()),
            baseRedirectUrl = BaseUrl("base-url"),
            chainId = ChainId(1337L),
            customRpcUrl = null,
            createdAt = TestData.TIMESTAMP
        )
        private val CHAIN_SPEC = ChainSpec(PROJECT.chainId, PROJECT.customRpcUrl)
    }

    private val objectMapper = JsonConfig().objectMapper()

    @Test
    fun mustCorrectlySubmitAndCreateAssetSnapshotWhenMerkleTreeDoesNotAlreadyExist() {
        val blockchainService = mock<BlockchainService>()
        val assetContractAddress = ContractAddress("a")
        val startBlock = BlockNumber(BigInteger("6"))

        suppose("contract deployment transaction is returned") {
            call(blockchainService.findContractDeploymentTransaction(CHAIN_SPEC, assetContractAddress, emptyList()))
                .willReturn(
                    FullContractDeploymentTransactionInfo(
                        hash = TransactionHash("hash"),
                        from = ZeroAddress.toWalletAddress(),
                        deployedContractAddress = assetContractAddress,
                        data = FunctionData("00"),
                        value = Balance.ZERO,
                        binary = ContractBinaryData("00"),
                        blockNumber = startBlock,
                        events = emptyList()
                    )
                )
        }

        val assetSnapshotRepository = mock<AssetSnapshotRepository>()
        val payoutBlock = BlockNumber(BigInteger.TEN)
        val ignoredHolderAddresses = setOf(WalletAddress("dead"))
        val assetSnapshotUuid = AssetSnapshotId(UUID.randomUUID())
        val name = "asset-snapshot-name"
        val params = CreateAssetSnapshotParams(
            name = name,
            chainId = PROJECT.chainId,
            projectId = PROJECT.id,
            assetContractAddress = assetContractAddress,
            payoutBlock = payoutBlock,
            ignoredHolderAddresses = ignoredHolderAddresses
        )

        suppose("asset snapshot is created in database") {
            call(assetSnapshotRepository.createAssetSnapshot(params))
                .willReturn(assetSnapshotUuid)
        }

        suppose("pending asset snapshot will be returned") {
            call(assetSnapshotRepository.getPending())
                .willReturn(
                    PendingAssetSnapshot(
                        id = assetSnapshotUuid,
                        projectId = PROJECT.id,
                        name = name,
                        chainId = PROJECT.chainId,
                        assetContractAddress = assetContractAddress,
                        blockNumber = payoutBlock,
                        ignoredHolderAddresses = ignoredHolderAddresses
                    )
                )
        }

        val accountBalances = listOf(
            PayoutAccountBalance(WalletAddress("2"), Balance(BigInteger.ONE)),
            PayoutAccountBalance(WalletAddress("3"), Balance(BigInteger.TWO))
        )
        val totalAssetAmount = Balance(BigInteger("3"))

        suppose("some asset balances are fetched") {
            call(
                blockchainService.fetchErc20AccountBalances(
                    chainSpec = CHAIN_SPEC,
                    erc20ContractAddress = assetContractAddress,
                    ignoredErc20Addresses = ignoredHolderAddresses,
                    startBlock = startBlock,
                    endBlock = payoutBlock
                )
            ).willReturn(accountBalances)
        }

        val tree = MerkleTree(accountBalances, HashFunction.KECCAK_256)
        val ipfsService = mock<IpfsService>()
        val ipfsHash = IpfsHash("testIpfsHash")

        suppose("Merkle tree is stored to IPFS") {
            call(ipfsService.pinJsonToIpfs(objectMapper.valueToTree(tree)))
                .willReturn(ipfsHash)
        }

        val merkleTreeRepository = mock<MerkleTreeRepository>()

        suppose("Merkle tree does not exist in the database") {
            call(
                merkleTreeRepository.fetchTree(
                    FetchMerkleTreeParams(
                        rootHash = tree.root.hash,
                        chainId = PROJECT.chainId,
                        assetContractAddress = assetContractAddress
                    )
                )
            )
                .willReturn(null)
        }

        val treeUuid = MerkleTreeRootId(UUID.randomUUID())

        suppose("Merkle tree is stored in the database and tree ID is returned") {
            call(merkleTreeRepository.storeTree(tree, PROJECT.chainId, assetContractAddress, payoutBlock))
                .willReturn(treeUuid)
        }

        suppose("Merkle tree can be fetched by ID") {
            call(merkleTreeRepository.getById(treeUuid))
                .willReturn(tree)
        }

        val properties = PayoutQueueProperties()
        val executorServiceProvider = mock<ScheduledExecutorServiceProvider>()
        val scheduler = ManualFixedScheduler()

        suppose("ManualFixedScheduler will be used") {
            call(executorServiceProvider.newSingleThreadScheduledExecutor(any()))
                .willReturn(scheduler)
        }

        val projectRepository = mock<ProjectRepository>()

        suppose("project will be returned") {
            call(projectRepository.getById(PROJECT.id))
                .willReturn(PROJECT)
        }

        val service = AssetSnapshotQueueServiceImpl(
            merkleTreeRepository = merkleTreeRepository,
            assetSnapshotRepository = assetSnapshotRepository,
            projectRepository = projectRepository,
            ipfsService = ipfsService,
            blockchainService = blockchainService,
            payoutQueueProperties = properties,
            objectMapper = objectMapper,
            scheduledExecutorServiceProvider = executorServiceProvider
        )

        verify("asset snapshot is submitted and correct asset snapshot ID is returned") {
            val response = service.submitAssetSnapshot(params)

            expectThat(response)
                .isEqualTo(assetSnapshotUuid)
        }

        suppose("asset snapshot is processed") {
            scheduler.execute()
        }

        suppose("successful asset snapshot is returned from database") {
            call(assetSnapshotRepository.getById(assetSnapshotUuid))
                .willReturn(
                    AssetSnapshot(
                        id = assetSnapshotUuid,
                        projectId = PROJECT.id,
                        name = name,
                        chainId = PROJECT.chainId,
                        assetContractAddress = assetContractAddress,
                        blockNumber = payoutBlock,
                        ignoredHolderAddresses = ignoredHolderAddresses,
                        data = SuccessfulAssetSnapshotData(
                            merkleTreeRootId = treeUuid,
                            merkleTreeIpfsHash = ipfsHash,
                            totalAssetAmount = totalAssetAmount
                        )
                    )
                )
        }

        verify("asset snapshot is successfully processed") {
            val response = service.getAssetSnapshotById(assetSnapshotUuid)

            expectThat(response)
                .isEqualTo(
                    FullAssetSnapshot(
                        id = assetSnapshotUuid,
                        projectId = PROJECT.id,
                        name = name,
                        chainId = PROJECT.chainId,
                        assetContractAddress = assetContractAddress,
                        blockNumber = payoutBlock,
                        ignoredHolderAddresses = ignoredHolderAddresses,
                        snapshotStatus = AssetSnapshotStatus.SUCCESS,
                        snapshotFailureCause = null,
                        data = FullAssetSnapshotData(
                            totalAssetAmount = totalAssetAmount,
                            merkleRootHash = tree.root.hash,
                            merkleTreeIpfsHash = ipfsHash,
                            merkleTreeDepth = tree.root.depth,
                            hashFn = tree.hashFn
                        )
                    )
                )
        }

        verify("correct service and repository calls are made") {
            expectInteractions(assetSnapshotRepository) {
                // submitSnapshot()
                once.createAssetSnapshot(params)
                // processSnapshots()
                once.getPending()
                // handlePendingSnapshot()
                once.completeAssetSnapshot(assetSnapshotUuid, treeUuid, ipfsHash, totalAssetAmount)
                // getSnapshotById()
                once.getById(assetSnapshotUuid)
            }

            expectInteractions(blockchainService) {
                // handlePendingSnapshot()
                once.findContractDeploymentTransaction(CHAIN_SPEC, assetContractAddress, emptyList())
                once.fetchErc20AccountBalances(
                    chainSpec = CHAIN_SPEC,
                    erc20ContractAddress = assetContractAddress,
                    ignoredErc20Addresses = ignoredHolderAddresses,
                    startBlock = startBlock,
                    endBlock = payoutBlock
                )
            }

            expectInteractions(merkleTreeRepository) {
                // handlePendingSnapshot()
                once.fetchTree(FetchMerkleTreeParams(tree.root.hash, PROJECT.chainId, assetContractAddress))
                once.storeTree(tree, PROJECT.chainId, assetContractAddress, payoutBlock)
                // getSnapshotById()
                once.getById(treeUuid)
            }

            expectInteractions(ipfsService) {
                // handlePendingSnapshot()
                once.pinJsonToIpfs(objectMapper.valueToTree(tree))
            }
        }
    }

    @Test
    fun mustFailAssetSnapshotWhenExceptionIsThrownDuringProcessing() {
        val blockchainService = mock<BlockchainService>()
        val assetContractAddress = ContractAddress("a")
        val startBlock = BlockNumber(BigInteger("6"))

        suppose("contract deployment transaction is returned") {
            call(blockchainService.findContractDeploymentTransaction(CHAIN_SPEC, assetContractAddress, emptyList()))
                .willReturn(
                    FullContractDeploymentTransactionInfo(
                        hash = TransactionHash("hash"),
                        from = ZeroAddress.toWalletAddress(),
                        deployedContractAddress = assetContractAddress,
                        data = FunctionData("00"),
                        value = Balance.ZERO,
                        binary = ContractBinaryData("00"),
                        blockNumber = startBlock,
                        events = emptyList()
                    )
                )
        }

        val assetSnapshotRepository = mock<AssetSnapshotRepository>()
        val payoutBlock = BlockNumber(BigInteger.TEN)
        val ignoredHolderAddresses = setOf(WalletAddress("dead"))
        val assetSnapshotUuid = AssetSnapshotId(UUID.randomUUID())
        val name = "asset-snapshot-name"
        val params = CreateAssetSnapshotParams(
            name = name,
            projectId = PROJECT.id,
            chainId = PROJECT.chainId,
            assetContractAddress = assetContractAddress,
            payoutBlock = payoutBlock,
            ignoredHolderAddresses = ignoredHolderAddresses
        )

        suppose("asset snapshot is created in database") {
            call(assetSnapshotRepository.createAssetSnapshot(params))
                .willReturn(assetSnapshotUuid)
        }

        suppose("pending asset snapshot will be returned") {
            call(assetSnapshotRepository.getPending())
                .willReturn(
                    PendingAssetSnapshot(
                        id = assetSnapshotUuid,
                        name = name,
                        projectId = PROJECT.id,
                        chainId = PROJECT.chainId,
                        assetContractAddress = assetContractAddress,
                        blockNumber = payoutBlock,
                        ignoredHolderAddresses = ignoredHolderAddresses
                    )
                )
        }

        suppose("fetching asset balances throws exception") {
            call(
                blockchainService.fetchErc20AccountBalances(
                    chainSpec = CHAIN_SPEC,
                    erc20ContractAddress = assetContractAddress,
                    ignoredErc20Addresses = ignoredHolderAddresses,
                    startBlock = startBlock,
                    endBlock = payoutBlock
                )
            ).willThrow(RuntimeException("test"))
        }

        val properties = PayoutQueueProperties()
        val executorServiceProvider = mock<ScheduledExecutorServiceProvider>()
        val scheduler = ManualFixedScheduler()

        suppose("ManualFixedScheduler will be used") {
            call(executorServiceProvider.newSingleThreadScheduledExecutor(any()))
                .willReturn(scheduler)
        }

        val projectRepository = mock<ProjectRepository>()

        suppose("project will be returned") {
            call(projectRepository.getById(PROJECT.id))
                .willReturn(PROJECT)
        }

        val merkleTreeRepository = mock<MerkleTreeRepository>()
        val ipfsService = mock<IpfsService>()

        val service = AssetSnapshotQueueServiceImpl(
            merkleTreeRepository = merkleTreeRepository,
            assetSnapshotRepository = assetSnapshotRepository,
            projectRepository = projectRepository,
            ipfsService = ipfsService,
            blockchainService = blockchainService,
            payoutQueueProperties = properties,
            objectMapper = objectMapper,
            scheduledExecutorServiceProvider = executorServiceProvider
        )

        verify("asset snapshot is submitted and correct asset snapshot ID is returned") {
            val response = service.submitAssetSnapshot(params)

            expectThat(response)
                .isEqualTo(assetSnapshotUuid)
        }

        suppose("asset snapshot is processed") {
            scheduler.execute()
        }

        suppose("failed asset snapshot is returned from database") {
            call(assetSnapshotRepository.getById(assetSnapshotUuid))
                .willReturn(
                    AssetSnapshot(
                        id = assetSnapshotUuid,
                        name = name,
                        projectId = PROJECT.id,
                        chainId = PROJECT.chainId,
                        assetContractAddress = assetContractAddress,
                        blockNumber = payoutBlock,
                        ignoredHolderAddresses = ignoredHolderAddresses,
                        data = OtherAssetSnapshotData(AssetSnapshotStatus.FAILED, AssetSnapshotFailureCause.OTHER)
                    )
                )
        }

        verify("asset snapshot processing failed") {
            val response = service.getAssetSnapshotById(assetSnapshotUuid)

            expectThat(response)
                .isEqualTo(
                    FullAssetSnapshot(
                        id = assetSnapshotUuid,
                        name = name,
                        projectId = PROJECT.id,
                        chainId = PROJECT.chainId,
                        assetContractAddress = assetContractAddress,
                        blockNumber = payoutBlock,
                        ignoredHolderAddresses = ignoredHolderAddresses,
                        snapshotStatus = AssetSnapshotStatus.FAILED,
                        snapshotFailureCause = AssetSnapshotFailureCause.OTHER,
                        data = null
                    )
                )
        }

        verify("correct service and repository calls are made") {
            expectInteractions(assetSnapshotRepository) {
                // submitSnapshot()
                once.createAssetSnapshot(params)
                // processSnapshots()
                once.getPending()
                // handlePendingSnapshot()
                once.failAssetSnapshot(assetSnapshotUuid, AssetSnapshotFailureCause.OTHER)
                // getSnapshotById()
                once.getById(assetSnapshotUuid)
            }

            expectInteractions(blockchainService) {
                // handlePendingSnapshot()
                once.findContractDeploymentTransaction(CHAIN_SPEC, assetContractAddress, emptyList())
                once.fetchErc20AccountBalances(
                    chainSpec = CHAIN_SPEC,
                    erc20ContractAddress = assetContractAddress,
                    ignoredErc20Addresses = ignoredHolderAddresses,
                    startBlock = startBlock,
                    endBlock = payoutBlock
                )
            }

            expectNoInteractions(merkleTreeRepository)
            expectNoInteractions(ipfsService)
        }
    }

    @Test
    fun mustFailAssetSnapshotWhenExceptionWithExceededLogSizeLimitIsThrownDuringProcessing() {
        val blockchainService = mock<BlockchainService>()
        val assetContractAddress = ContractAddress("a")
        val startBlock = BlockNumber(BigInteger("6"))

        suppose("contract deployment transaction is returned") {
            call(blockchainService.findContractDeploymentTransaction(CHAIN_SPEC, assetContractAddress, emptyList()))
                .willReturn(
                    FullContractDeploymentTransactionInfo(
                        hash = TransactionHash("hash"),
                        from = ZeroAddress.toWalletAddress(),
                        deployedContractAddress = assetContractAddress,
                        data = FunctionData("00"),
                        value = Balance.ZERO,
                        binary = ContractBinaryData("00"),
                        blockNumber = startBlock,
                        events = emptyList()
                    )
                )
        }

        val assetSnapshotRepository = mock<AssetSnapshotRepository>()
        val payoutBlock = BlockNumber(BigInteger.TEN)
        val ignoredHolderAddresses = setOf(WalletAddress("dead"))
        val assetSnapshotUuid = AssetSnapshotId(UUID.randomUUID())
        val name = "asset-snapshot-name"
        val params = CreateAssetSnapshotParams(
            name = name,
            chainId = PROJECT.chainId,
            projectId = PROJECT.id,
            assetContractAddress = assetContractAddress,
            payoutBlock = payoutBlock,
            ignoredHolderAddresses = ignoredHolderAddresses
        )

        suppose("asset snapshot is created in database") {
            call(assetSnapshotRepository.createAssetSnapshot(params))
                .willReturn(assetSnapshotUuid)
        }

        suppose("pending asset snapshot will be returned") {
            call(assetSnapshotRepository.getPending())
                .willReturn(
                    PendingAssetSnapshot(
                        id = assetSnapshotUuid,
                        name = name,
                        chainId = PROJECT.chainId,
                        projectId = PROJECT.id,
                        assetContractAddress = assetContractAddress,
                        blockNumber = payoutBlock,
                        ignoredHolderAddresses = ignoredHolderAddresses
                    )
                )
        }

        suppose("fetching asset balances throws exception") {
            call(
                blockchainService.fetchErc20AccountBalances(
                    chainSpec = CHAIN_SPEC,
                    erc20ContractAddress = assetContractAddress,
                    ignoredErc20Addresses = ignoredHolderAddresses,
                    startBlock = startBlock,
                    endBlock = payoutBlock
                )
            ).willThrow(RuntimeException(RuntimeException("Log response size exceeded")))
        }

        val properties = PayoutQueueProperties()
        val executorServiceProvider = mock<ScheduledExecutorServiceProvider>()
        val scheduler = ManualFixedScheduler()

        suppose("ManualFixedScheduler will be used") {
            call(executorServiceProvider.newSingleThreadScheduledExecutor(any()))
                .willReturn(scheduler)
        }

        val projectRepository = mock<ProjectRepository>()

        suppose("project will be returned") {
            call(projectRepository.getById(PROJECT.id))
                .willReturn(PROJECT)
        }

        val merkleTreeRepository = mock<MerkleTreeRepository>()
        val ipfsService = mock<IpfsService>()

        val service = AssetSnapshotQueueServiceImpl(
            merkleTreeRepository = merkleTreeRepository,
            assetSnapshotRepository = assetSnapshotRepository,
            projectRepository = projectRepository,
            ipfsService = ipfsService,
            blockchainService = blockchainService,
            payoutQueueProperties = properties,
            objectMapper = objectMapper,
            scheduledExecutorServiceProvider = executorServiceProvider
        )

        verify("asset snapshot is submitted and correct asset snapshot ID is returned") {
            val response = service.submitAssetSnapshot(params)

            expectThat(response)
                .isEqualTo(assetSnapshotUuid)
        }

        suppose("asset snapshot is processed") {
            scheduler.execute()
        }

        suppose("failed asset snapshot is returned from database") {
            call(assetSnapshotRepository.getById(assetSnapshotUuid))
                .willReturn(
                    AssetSnapshot(
                        id = assetSnapshotUuid,
                        name = name,
                        chainId = PROJECT.chainId,
                        projectId = PROJECT.id,
                        assetContractAddress = assetContractAddress,
                        blockNumber = payoutBlock,
                        ignoredHolderAddresses = ignoredHolderAddresses,
                        data = OtherAssetSnapshotData(
                            status = AssetSnapshotStatus.FAILED,
                            failureCause = AssetSnapshotFailureCause.LOG_RESPONSE_LIMIT
                        )
                    )
                )
        }

        verify("asset snapshot processing failed") {
            val response = service.getAssetSnapshotById(assetSnapshotUuid)

            expectThat(response)
                .isEqualTo(
                    FullAssetSnapshot(
                        id = assetSnapshotUuid,
                        name = name,
                        chainId = PROJECT.chainId,
                        projectId = PROJECT.id,
                        assetContractAddress = assetContractAddress,
                        blockNumber = payoutBlock,
                        ignoredHolderAddresses = ignoredHolderAddresses,
                        snapshotStatus = AssetSnapshotStatus.FAILED,
                        snapshotFailureCause = AssetSnapshotFailureCause.LOG_RESPONSE_LIMIT,
                        data = null
                    )
                )
        }

        verify("correct service and repository calls are made") {
            expectInteractions(assetSnapshotRepository) {
                // submitSnapshot()
                once.createAssetSnapshot(params)
                // processSnapshots()
                once.getPending()
                // handlePendingSnapshot()
                once.failAssetSnapshot(assetSnapshotUuid, AssetSnapshotFailureCause.LOG_RESPONSE_LIMIT)
                // getSnapshotById()
                once.getById(assetSnapshotUuid)
            }

            expectInteractions(blockchainService) {
                // handlePendingSnapshot()
                once.findContractDeploymentTransaction(CHAIN_SPEC, assetContractAddress, emptyList())
                once.fetchErc20AccountBalances(
                    chainSpec = CHAIN_SPEC,
                    erc20ContractAddress = assetContractAddress,
                    ignoredErc20Addresses = ignoredHolderAddresses,
                    startBlock = startBlock,
                    endBlock = payoutBlock
                )
            }

            expectNoInteractions(merkleTreeRepository)
            expectNoInteractions(ipfsService)
        }
    }

    @Test
    fun mustCorrectlySubmitAndCreateAssetSnapshotWhenMerkleTreeAlreadyExist() {
        val blockchainService = mock<BlockchainService>()
        val assetContractAddress = ContractAddress("a")
        val startBlock = BlockNumber(BigInteger("6"))

        suppose("contract deployment transaction is returned") {
            call(blockchainService.findContractDeploymentTransaction(CHAIN_SPEC, assetContractAddress, emptyList()))
                .willReturn(
                    FullContractDeploymentTransactionInfo(
                        hash = TransactionHash("hash"),
                        from = ZeroAddress.toWalletAddress(),
                        deployedContractAddress = assetContractAddress,
                        data = FunctionData("00"),
                        value = Balance.ZERO,
                        binary = ContractBinaryData("00"),
                        blockNumber = startBlock,
                        events = emptyList()
                    )
                )
        }

        val assetSnapshotRepository = mock<AssetSnapshotRepository>()
        val payoutBlock = BlockNumber(BigInteger.TEN)
        val ignoredHolderAddresses = setOf(WalletAddress("dead"))
        val assetSnapshotUuid = AssetSnapshotId(UUID.randomUUID())
        val name = "asset-snapshot-name"
        val params = CreateAssetSnapshotParams(
            name = name,
            chainId = PROJECT.chainId,
            projectId = PROJECT.id,
            assetContractAddress = assetContractAddress,
            payoutBlock = payoutBlock,
            ignoredHolderAddresses = ignoredHolderAddresses
        )

        suppose("asset snapshot is created in database") {
            call(assetSnapshotRepository.createAssetSnapshot(params))
                .willReturn(assetSnapshotUuid)
        }

        suppose("pending asset snapshot will be returned") {
            call(assetSnapshotRepository.getPending())
                .willReturn(
                    PendingAssetSnapshot(
                        id = assetSnapshotUuid,
                        name = name,
                        chainId = PROJECT.chainId,
                        projectId = PROJECT.id,
                        assetContractAddress = assetContractAddress,
                        blockNumber = payoutBlock,
                        ignoredHolderAddresses = ignoredHolderAddresses
                    )
                )
        }

        val accountBalances = listOf(
            PayoutAccountBalance(WalletAddress("2"), Balance(BigInteger.ONE)),
            PayoutAccountBalance(WalletAddress("3"), Balance(BigInteger.TWO))
        )
        val totalAssetAmount = Balance(BigInteger("3"))

        suppose("some asset balances are fetched") {
            call(
                blockchainService.fetchErc20AccountBalances(
                    chainSpec = CHAIN_SPEC,
                    erc20ContractAddress = assetContractAddress,
                    ignoredErc20Addresses = ignoredHolderAddresses,
                    startBlock = startBlock,
                    endBlock = payoutBlock
                )
            ).willReturn(accountBalances)
        }

        val tree = MerkleTree(accountBalances, HashFunction.KECCAK_256)
        val ipfsService = mock<IpfsService>()
        val ipfsHash = IpfsHash("testIpfsHash")

        suppose("Merkle tree is stored to IPFS") {
            call(ipfsService.pinJsonToIpfs(objectMapper.valueToTree(tree)))
                .willReturn(ipfsHash)
        }

        val merkleTreeRepository = mock<MerkleTreeRepository>()
        val treeUuid = MerkleTreeRootId(UUID.randomUUID())

        suppose("Merkle tree exists in the database") {
            call(
                merkleTreeRepository.fetchTree(
                    FetchMerkleTreeParams(
                        rootHash = tree.root.hash,
                        chainId = PROJECT.chainId,
                        assetContractAddress = assetContractAddress
                    )
                )
            )
                .willReturn(MerkleTreeWithId(treeUuid, tree))
        }

        suppose("Merkle tree is stored in the database and tree ID is returned") {
            call(merkleTreeRepository.storeTree(tree, PROJECT.chainId, assetContractAddress, payoutBlock))
                .willReturn(treeUuid)
        }

        suppose("Merkle tree can be fetched by ID") {
            call(merkleTreeRepository.getById(treeUuid))
                .willReturn(tree)
        }

        val properties = PayoutQueueProperties()
        val executorServiceProvider = mock<ScheduledExecutorServiceProvider>()
        val scheduler = ManualFixedScheduler()

        suppose("ManualFixedScheduler will be used") {
            call(executorServiceProvider.newSingleThreadScheduledExecutor(any()))
                .willReturn(scheduler)
        }

        val projectRepository = mock<ProjectRepository>()

        suppose("project will be returned") {
            call(projectRepository.getById(PROJECT.id))
                .willReturn(PROJECT)
        }

        val service = AssetSnapshotQueueServiceImpl(
            merkleTreeRepository = merkleTreeRepository,
            assetSnapshotRepository = assetSnapshotRepository,
            projectRepository = projectRepository,
            ipfsService = ipfsService,
            blockchainService = blockchainService,
            payoutQueueProperties = properties,
            objectMapper = objectMapper,
            scheduledExecutorServiceProvider = executorServiceProvider
        )

        verify("asset snapshot is submitted and correct snapshot ID is returned") {
            val response = service.submitAssetSnapshot(params)

            expectThat(response)
                .isEqualTo(assetSnapshotUuid)
        }

        suppose("asset snapshot is processed") {
            scheduler.execute()
        }

        suppose("successful asset snapshot is returned from database") {
            call(assetSnapshotRepository.getById(assetSnapshotUuid))
                .willReturn(
                    AssetSnapshot(
                        id = assetSnapshotUuid,
                        name = name,
                        chainId = PROJECT.chainId,
                        projectId = PROJECT.id,
                        assetContractAddress = assetContractAddress,
                        blockNumber = payoutBlock,
                        ignoredHolderAddresses = ignoredHolderAddresses,
                        data = SuccessfulAssetSnapshotData(
                            merkleTreeRootId = treeUuid,
                            merkleTreeIpfsHash = ipfsHash,
                            totalAssetAmount = totalAssetAmount
                        )
                    )
                )
        }

        verify("asset snapshot is successfully processed") {
            val response = service.getAssetSnapshotById(assetSnapshotUuid)

            expectThat(response)
                .isEqualTo(
                    FullAssetSnapshot(
                        id = assetSnapshotUuid,
                        name = name,
                        chainId = PROJECT.chainId,
                        projectId = PROJECT.id,
                        assetContractAddress = assetContractAddress,
                        blockNumber = payoutBlock,
                        ignoredHolderAddresses = ignoredHolderAddresses,
                        snapshotStatus = AssetSnapshotStatus.SUCCESS,
                        snapshotFailureCause = null,
                        data = FullAssetSnapshotData(
                            totalAssetAmount = totalAssetAmount,
                            merkleRootHash = tree.root.hash,
                            merkleTreeIpfsHash = ipfsHash,
                            merkleTreeDepth = tree.root.depth,
                            hashFn = tree.hashFn
                        )
                    )
                )
        }

        verify("correct service and repository calls are made") {
            expectInteractions(assetSnapshotRepository) {
                // submitSnapshot()
                once.createAssetSnapshot(params)
                // processSnapshots()
                once.getPending()
                // handlePendingSnapshot()
                once.completeAssetSnapshot(assetSnapshotUuid, treeUuid, ipfsHash, totalAssetAmount)
                // getSnapshotById()
                once.getById(assetSnapshotUuid)
            }

            expectInteractions(blockchainService) {
                // handlePendingSnapshot()
                once.findContractDeploymentTransaction(CHAIN_SPEC, assetContractAddress, emptyList())
                once.fetchErc20AccountBalances(
                    chainSpec = CHAIN_SPEC,
                    erc20ContractAddress = assetContractAddress,
                    ignoredErc20Addresses = ignoredHolderAddresses,
                    startBlock = startBlock,
                    endBlock = payoutBlock
                )
            }

            expectInteractions(merkleTreeRepository) {
                // handlePendingSnapshot()
                once.fetchTree(FetchMerkleTreeParams(tree.root.hash, PROJECT.chainId, assetContractAddress))
                // getSnapshotById()
                once.getById(treeUuid)
            }

            expectInteractions(ipfsService) {
                // handlePendingSnapshot()
                once.pinJsonToIpfs(objectMapper.valueToTree(tree))
            }
        }
    }

    @Test
    fun mustCorrectlyFetchAllAssetSnapshotsByProjectIdAndStatuses() {
        val merkleTreeRepository = mock<MerkleTreeRepository>()
        val treeUuid = MerkleTreeRootId(UUID.randomUUID())
        val tree = MerkleTree(
            listOf(PayoutAccountBalance(WalletAddress("aaaa"), Balance(BigInteger.ONE))),
            HashFunction.KECCAK_256
        )

        suppose("Merkle tree can be fetched by ID") {
            call(merkleTreeRepository.getById(treeUuid))
                .willReturn(tree)
        }

        val assetSnapshotRepository = mock<AssetSnapshotRepository>()
        val ipfsHash = IpfsHash("ipfs-hash")
        val totalAssetAmount = Balance(BigInteger("1000"))
        val assetSnapshots = listOf(
            AssetSnapshot(
                id = AssetSnapshotId(UUID.randomUUID()),
                name = "snapshot-1",
                chainId = PROJECT.chainId,
                projectId = PROJECT.id,
                assetContractAddress = ContractAddress("1"),
                blockNumber = BlockNumber(BigInteger.TEN),
                ignoredHolderAddresses = emptySet(),
                data = SuccessfulAssetSnapshotData(
                    merkleTreeRootId = treeUuid,
                    merkleTreeIpfsHash = ipfsHash,
                    totalAssetAmount = totalAssetAmount
                )
            ),
            AssetSnapshot(
                id = AssetSnapshotId(UUID.randomUUID()),
                name = "snapshot-2",
                chainId = PROJECT.chainId,
                projectId = PROJECT.id,
                assetContractAddress = ContractAddress("2"),
                blockNumber = BlockNumber(BigInteger.TEN),
                ignoredHolderAddresses = emptySet(),
                data = OtherAssetSnapshotData(AssetSnapshotStatus.PENDING, null)
            )
        )
        val statuses = setOf(AssetSnapshotStatus.PENDING, AssetSnapshotStatus.SUCCESS)

        suppose("some asset snapshots are returned") {
            call(assetSnapshotRepository.getAllByProjectIdAndStatuses(PROJECT.id, statuses))
                .willReturn(assetSnapshots)
        }

        val executorServiceProvider = mock<ScheduledExecutorServiceProvider>()
        val scheduler = ManualFixedScheduler()

        suppose("ManualFixedScheduler will be used") {
            call(executorServiceProvider.newSingleThreadScheduledExecutor(any()))
                .willReturn(scheduler)
        }

        val projectRepository = mock<ProjectRepository>()

        suppose("project will be returned") {
            call(projectRepository.getById(PROJECT.id))
                .willReturn(PROJECT)
        }

        val service = AssetSnapshotQueueServiceImpl(
            merkleTreeRepository = merkleTreeRepository,
            assetSnapshotRepository = assetSnapshotRepository,
            projectRepository = projectRepository,
            ipfsService = mock(),
            blockchainService = mock(),
            payoutQueueProperties = PayoutQueueProperties(),
            objectMapper = objectMapper,
            scheduledExecutorServiceProvider = executorServiceProvider
        )

        verify("asset snapshots are correctly fetched by projectId and statuses") {
            val response = service.getAllAssetSnapshotsByProjectIdAndStatuses(PROJECT.id, statuses)

            expectThat(response)
                .containsExactlyInAnyOrder(
                    FullAssetSnapshot(
                        id = assetSnapshots[0].id,
                        name = assetSnapshots[0].name,
                        chainId = assetSnapshots[0].chainId,
                        projectId = assetSnapshots[0].projectId,
                        assetContractAddress = assetSnapshots[0].assetContractAddress,
                        blockNumber = assetSnapshots[0].blockNumber,
                        ignoredHolderAddresses = emptySet(),
                        snapshotStatus = AssetSnapshotStatus.SUCCESS,
                        snapshotFailureCause = null,
                        data = FullAssetSnapshotData(
                            totalAssetAmount = totalAssetAmount,
                            merkleRootHash = tree.root.hash,
                            merkleTreeIpfsHash = ipfsHash,
                            merkleTreeDepth = tree.root.depth,
                            hashFn = tree.hashFn
                        )
                    ),
                    FullAssetSnapshot(
                        id = assetSnapshots[1].id,
                        name = assetSnapshots[1].name,
                        chainId = assetSnapshots[1].chainId,
                        projectId = assetSnapshots[1].projectId,
                        assetContractAddress = assetSnapshots[1].assetContractAddress,
                        blockNumber = assetSnapshots[1].blockNumber,
                        ignoredHolderAddresses = emptySet(),
                        snapshotStatus = AssetSnapshotStatus.PENDING,
                        snapshotFailureCause = null,
                        data = null
                    )
                )
        }
    }
}
