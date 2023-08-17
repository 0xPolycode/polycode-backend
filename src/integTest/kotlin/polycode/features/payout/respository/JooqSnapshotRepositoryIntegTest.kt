package polycode.features.payout.respository

import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import polycode.TestBase
import polycode.TestData
import polycode.config.DatabaseConfig
import polycode.features.payout.model.params.CreateAssetSnapshotParams
import polycode.features.payout.model.result.AssetSnapshot
import polycode.features.payout.model.result.OtherAssetSnapshotData
import polycode.features.payout.model.result.PendingAssetSnapshot
import polycode.features.payout.model.result.SuccessfulAssetSnapshotData
import polycode.features.payout.repository.AssetSnapshotRepository
import polycode.features.payout.repository.JooqAssetSnapshotRepository
import polycode.features.payout.util.AssetSnapshotFailureCause
import polycode.features.payout.util.AssetSnapshotStatus
import polycode.features.payout.util.HashFunction
import polycode.features.payout.util.IpfsHash
import polycode.features.payout.util.MerkleHash
import polycode.generated.jooq.enums.UserIdentifierType
import polycode.generated.jooq.id.AssetSnapshotId
import polycode.generated.jooq.id.MerkleTreeRootId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import polycode.generated.jooq.tables.AssetSnapshotTable
import polycode.generated.jooq.tables.records.AssetSnapshotRecord
import polycode.generated.jooq.tables.records.MerkleTreeRootRecord
import polycode.generated.jooq.tables.records.ProjectRecord
import polycode.generated.jooq.tables.records.UserIdentifierRecord
import polycode.service.UuidProvider
import polycode.testcontainers.PostgresTestContainer
import polycode.util.Balance
import polycode.util.BaseUrl
import polycode.util.BlockNumber
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.WalletAddress
import java.math.BigInteger
import java.util.UUID

@JooqTest
@Import(JooqAssetSnapshotRepository::class, DatabaseConfig::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqSnapshotRepositoryIntegTest : TestBase() {

    companion object {
        private val PROJECT_ID_1 = ProjectId(UUID.randomUUID())
        private val PROJECT_ID_2 = ProjectId(UUID.randomUUID())
        private val OWNER_ID = UserId(UUID.randomUUID())
    }

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)

        dslContext.executeInsert(
            UserIdentifierRecord(
                id = OWNER_ID,
                userIdentifier = "user-identifier",
                identifierType = UserIdentifierType.ETH_WALLET_ADDRESS
            )
        )

        dslContext.executeInsert(
            ProjectRecord(
                id = PROJECT_ID_1,
                ownerId = OWNER_ID,
                baseRedirectUrl = BaseUrl("base-redirect-url-1"),
                chainId = ChainId(1337L),
                customRpcUrl = "custom-rpc-url-1",
                createdAt = TestData.TIMESTAMP
            )
        )

        dslContext.executeInsert(
            ProjectRecord(
                id = PROJECT_ID_2,
                ownerId = OWNER_ID,
                baseRedirectUrl = BaseUrl("base-redirect-url-2"),
                chainId = ChainId(1337L),
                customRpcUrl = "custom-rpc-url-2",
                createdAt = TestData.TIMESTAMP
            )
        )
    }

    @Suppress("unused")
    private val postgresContainer = PostgresTestContainer()

    @Autowired
    private lateinit var repository: AssetSnapshotRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @MockBean
    private lateinit var uuidProvider: UuidProvider

    @Test
    fun mustCorrectlyFetchSuccessfulAssetSnapshotId() {
        val treeUuid = MerkleTreeRootId(UUID.randomUUID())
        val name = "asset-snapshot-name"
        val chainId = ChainId(1L)
        val assetContractAddress = ContractAddress("a")
        val payoutBlock = BlockNumber(BigInteger.TEN)
        val treeRootHash = MerkleHash("tree-root-hash")
        val hashFn = HashFunction.IDENTITY

        suppose("some Merkle tree root exists in database") {
            dslContext.executeInsert(
                MerkleTreeRootRecord(
                    id = treeUuid,
                    chainId = chainId,
                    assetContractAddress = assetContractAddress,
                    blockNumber = payoutBlock,
                    merkleHash = treeRootHash,
                    hashFn = hashFn
                )
            )
        }

        val assetSnapshotUuid = AssetSnapshotId(UUID.randomUUID())
        val ignoredHolderAddresses = setOf(WalletAddress("e"))
        val treeIpfsHash = IpfsHash("tree-ipfs-hash")
        val totalAssetAmount = Balance(BigInteger("123"))

        suppose("successful asset snapshot is stored into database") {
            dslContext.executeInsert(
                AssetSnapshotRecord(
                    id = assetSnapshotUuid,
                    name = name,
                    chainId = chainId,
                    projectId = PROJECT_ID_1,
                    assetContractAddress = assetContractAddress,
                    blockNumber = payoutBlock,
                    ignoredHolderAddresses = ignoredHolderAddresses.map { it.rawValue }.toTypedArray(),
                    status = AssetSnapshotStatus.SUCCESS,
                    failureCause = null,
                    resultTree = treeUuid,
                    treeIpfsHash = treeIpfsHash,
                    totalAssetAmount = totalAssetAmount
                )
            )
        }

        verify("successful asset snapshot is correctly fetched from database") {
            val result = repository.getById(assetSnapshotUuid)

            expectThat(result)
                .isEqualTo(
                    AssetSnapshot(
                        id = assetSnapshotUuid,
                        name = name,
                        chainId = chainId,
                        projectId = PROJECT_ID_1,
                        assetContractAddress = assetContractAddress,
                        blockNumber = payoutBlock,
                        ignoredHolderAddresses = ignoredHolderAddresses,
                        data = SuccessfulAssetSnapshotData(
                            merkleTreeRootId = treeUuid,
                            merkleTreeIpfsHash = treeIpfsHash,
                            totalAssetAmount = totalAssetAmount
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchPendingAssetSnapshotById() {
        val assetSnapshotUuid = AssetSnapshotId(UUID.randomUUID())
        val name = "asset-snapshot-name"
        val chainId = ChainId(1L)
        val assetContractAddress = ContractAddress("a")
        val payoutBlock = BlockNumber(BigInteger.TEN)
        val ignoredHolderAddresses = setOf(WalletAddress("e"))

        suppose("pending asset snapshot is stored into database") {
            dslContext.executeInsert(
                AssetSnapshotRecord(
                    id = assetSnapshotUuid,
                    name = name,
                    chainId = chainId,
                    projectId = PROJECT_ID_1,
                    assetContractAddress = assetContractAddress,
                    blockNumber = payoutBlock,
                    ignoredHolderAddresses = ignoredHolderAddresses.map { it.rawValue }.toTypedArray(),
                    status = AssetSnapshotStatus.PENDING,
                    failureCause = null,
                    resultTree = null,
                    treeIpfsHash = null,
                    totalAssetAmount = null
                )
            )
        }

        verify("pending asset snapshot is correctly fetched from database") {
            val result = repository.getById(assetSnapshotUuid)

            expectThat(result)
                .isEqualTo(
                    AssetSnapshot(
                        id = assetSnapshotUuid,
                        name = name,
                        chainId = chainId,
                        projectId = PROJECT_ID_1,
                        assetContractAddress = assetContractAddress,
                        blockNumber = payoutBlock,
                        ignoredHolderAddresses = ignoredHolderAddresses,
                        data = OtherAssetSnapshotData(AssetSnapshotStatus.PENDING, null)
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchFailedAssetSnapshotById() {
        val assetSnapshotUuid = AssetSnapshotId(UUID.randomUUID())
        val name = "asset-snapshot-name"
        val chainId = ChainId(1L)
        val assetContractAddress = ContractAddress("a")
        val payoutBlock = BlockNumber(BigInteger.TEN)
        val ignoredHolderAddresses = setOf(WalletAddress("e"))

        suppose("failed asset snapshot is stored into database") {
            dslContext.executeInsert(
                AssetSnapshotRecord(
                    id = assetSnapshotUuid,
                    name = name,
                    chainId = chainId,
                    projectId = PROJECT_ID_1,
                    assetContractAddress = assetContractAddress,
                    blockNumber = payoutBlock,
                    ignoredHolderAddresses = ignoredHolderAddresses.map { it.rawValue }.toTypedArray(),
                    status = AssetSnapshotStatus.FAILED,
                    failureCause = AssetSnapshotFailureCause.OTHER,
                    resultTree = null,
                    treeIpfsHash = null,
                    totalAssetAmount = null
                )
            )
        }

        verify("failed asset snapshot is correctly fetched from database") {
            val result = repository.getById(assetSnapshotUuid)

            expectThat(result)
                .isEqualTo(
                    AssetSnapshot(
                        id = assetSnapshotUuid,
                        name = name,
                        chainId = chainId,
                        projectId = PROJECT_ID_1,
                        assetContractAddress = assetContractAddress,
                        blockNumber = payoutBlock,
                        ignoredHolderAddresses = ignoredHolderAddresses,
                        data = OtherAssetSnapshotData(AssetSnapshotStatus.FAILED, AssetSnapshotFailureCause.OTHER)
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchAllAssetSnapshotsByProjectIdAndStatuses() {
        val project1AssetSnapshots = listOf(
            assetSnapshotRecord(PROJECT_ID_1, AssetSnapshotStatus.PENDING, null),
            assetSnapshotRecord(PROJECT_ID_1, AssetSnapshotStatus.FAILED, AssetSnapshotFailureCause.OTHER),
            assetSnapshotRecord(PROJECT_ID_1, AssetSnapshotStatus.PENDING, null),
            assetSnapshotRecord(PROJECT_ID_1, AssetSnapshotStatus.PENDING, null),
            assetSnapshotRecord(PROJECT_ID_1, AssetSnapshotStatus.FAILED, AssetSnapshotFailureCause.OTHER),
            assetSnapshotRecord(PROJECT_ID_1, AssetSnapshotStatus.FAILED, AssetSnapshotFailureCause.OTHER)
        )

        val project2AssetSnapshots = listOf(
            assetSnapshotRecord(PROJECT_ID_2, AssetSnapshotStatus.PENDING, null),
            assetSnapshotRecord(PROJECT_ID_2, AssetSnapshotStatus.PENDING, null),
            assetSnapshotRecord(PROJECT_ID_2, AssetSnapshotStatus.PENDING, null),
            assetSnapshotRecord(PROJECT_ID_2, AssetSnapshotStatus.PENDING, null)
        )

        val allAssetSnapshots = project1AssetSnapshots + project2AssetSnapshots

        suppose("all asset snapshots are stored into database") {
            dslContext.batchInsert(allAssetSnapshots).execute()
        }

        verify("asset snapshots are correctly fetched by projectId") {
            expectThat(repository.getAllByProjectIdAndStatuses(PROJECT_ID_1, emptySet()))
                .containsExactlyInAnyOrderElementsOf(project1AssetSnapshots.toModels())

            expectThat(repository.getAllByProjectIdAndStatuses(PROJECT_ID_2, emptySet()))
                .containsExactlyInAnyOrderElementsOf(project2AssetSnapshots.toModels())
        }

        verify("asset snapshots are correctly fetched by projectId and status") {
            expectThat(
                repository.getAllByProjectIdAndStatuses(PROJECT_ID_1, setOf(AssetSnapshotStatus.PENDING)) +
                    repository.getAllByProjectIdAndStatuses(PROJECT_ID_2, setOf(AssetSnapshotStatus.PENDING))
            )
                .containsExactlyInAnyOrderElementsOf(
                    allAssetSnapshots.filter { it.status == AssetSnapshotStatus.PENDING }.toModels()
                )

            expectThat(
                repository.getAllByProjectIdAndStatuses(PROJECT_ID_1, setOf(AssetSnapshotStatus.FAILED)) +
                    repository.getAllByProjectIdAndStatuses(PROJECT_ID_2, setOf(AssetSnapshotStatus.FAILED))
            )
                .containsExactlyInAnyOrderElementsOf(
                    allAssetSnapshots.filter { it.status == AssetSnapshotStatus.FAILED }.toModels()
                )
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentAssetSnapshotById() {
        verify("null is returned when fetching non-existent asset snapshot") {
            val result = repository.getById(AssetSnapshotId(UUID.randomUUID()))

            expectThat(result)
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyCreateAssetSnapshot() {
        val assetSnapshotUuid = AssetSnapshotId(UUID.randomUUID())

        suppose("UUID provider will return specified UUIDs") {
            call(uuidProvider.getUuid(AssetSnapshotId))
                .willReturn(assetSnapshotUuid)
        }

        val chainId = ChainId(1L)
        val name = "asset-snapshot-name"
        val assetContractAddress = ContractAddress("a")
        val payoutBlock = BlockNumber(BigInteger.TEN)
        val ignoredHolderAddresses = setOf(WalletAddress("e"))

        val storedAssetSnapshotId = suppose("asset snapshot is stored into database") {
            repository.createAssetSnapshot(
                CreateAssetSnapshotParams(
                    name = name,
                    chainId = chainId,
                    projectId = PROJECT_ID_1,
                    assetContractAddress = assetContractAddress,
                    payoutBlock = payoutBlock,
                    ignoredHolderAddresses = ignoredHolderAddresses
                )
            )
        }

        verify("correct asset snapshot ID is returned") {
            expectThat(storedAssetSnapshotId)
                .isEqualTo(assetSnapshotUuid)
        }

        verify("asset snapshot is correctly stored into database") {
            val record = dslContext.selectFrom(AssetSnapshotTable)
                .where(AssetSnapshotTable.ID.eq(assetSnapshotUuid))
                .fetchOne()

            expectThat(record)
                .isEqualTo(
                    AssetSnapshotRecord(
                        id = assetSnapshotUuid,
                        name = name,
                        chainId = chainId,
                        projectId = PROJECT_ID_1,
                        assetContractAddress = assetContractAddress,
                        blockNumber = payoutBlock,
                        ignoredHolderAddresses = ignoredHolderAddresses.map { it.rawValue }.toTypedArray(),
                        status = AssetSnapshotStatus.PENDING,
                        failureCause = null,
                        resultTree = null,
                        treeIpfsHash = null,
                        totalAssetAmount = null
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchSinglePendingAssetSnapshotFromDatabase() {
        val assetSnapshotUuid = AssetSnapshotId(UUID.randomUUID())
        val name = "asset-snapshot-name"
        val chainId = ChainId(1L)
        val assetContractAddress = ContractAddress("a")
        val payoutBlock = BlockNumber(BigInteger.TEN)
        val ignoredHolderAddresses = setOf(WalletAddress("e"))

        suppose("pending asset snapshot is stored into database") {
            dslContext.executeInsert(
                AssetSnapshotRecord(
                    id = assetSnapshotUuid,
                    name = name,
                    chainId = chainId,
                    projectId = PROJECT_ID_1,
                    assetContractAddress = assetContractAddress,
                    blockNumber = payoutBlock,
                    ignoredHolderAddresses = ignoredHolderAddresses.map { it.rawValue }.toTypedArray(),
                    status = AssetSnapshotStatus.PENDING,
                    failureCause = null,
                    resultTree = null,
                    treeIpfsHash = null,
                    totalAssetAmount = null
                )
            )
        }

        verify("single pending asset snapshot is fetched from database") {
            val result = repository.getPending()

            expectThat(result)
                .isEqualTo(
                    PendingAssetSnapshot(
                        id = assetSnapshotUuid,
                        name = name,
                        chainId = chainId,
                        projectId = PROJECT_ID_1,
                        assetContractAddress = assetContractAddress,
                        blockNumber = payoutBlock,
                        ignoredHolderAddresses = ignoredHolderAddresses
                    )
                )
        }
    }

    @Test
    fun mustReturnNullWhenThereAreNoPendingAssetSnapshots() {
        verify("null is returned when fetching single pending asset snapshot") {
            val result = repository.getPending()

            expectThat(result)
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyCompleteAssetSnapshot() {
        val assetSnapshotUuid = AssetSnapshotId(UUID.randomUUID())

        suppose("UUID provider will return specified UUIDs") {
            call(uuidProvider.getUuid(AssetSnapshotId))
                .willReturn(assetSnapshotUuid)
        }

        val name = "asset-snapshot-name"
        val chainId = ChainId(1L)
        val assetContractAddress = ContractAddress("a")
        val payoutBlock = BlockNumber(BigInteger.TEN)
        val ignoredHolderAddresses = setOf(WalletAddress("e"))

        val storedAssetSnapshotId = suppose("asset snapshot is stored into database") {
            repository.createAssetSnapshot(
                CreateAssetSnapshotParams(
                    name = name,
                    chainId = chainId,
                    projectId = PROJECT_ID_1,
                    assetContractAddress = assetContractAddress,
                    payoutBlock = payoutBlock,
                    ignoredHolderAddresses = ignoredHolderAddresses
                )
            )
        }

        verify("correct asset snapshot ID is returned") {
            expectThat(storedAssetSnapshotId)
                .isEqualTo(assetSnapshotUuid)
        }

        val treeUuid = MerkleTreeRootId(UUID.randomUUID())
        val treeRootHash = MerkleHash("tree-root-hash")
        val hashFn = HashFunction.IDENTITY

        suppose("some Merkle tree root exists in database") {
            dslContext.executeInsert(
                MerkleTreeRootRecord(
                    id = treeUuid,
                    chainId = chainId,
                    assetContractAddress = assetContractAddress,
                    blockNumber = payoutBlock,
                    merkleHash = treeRootHash,
                    hashFn = hashFn
                )
            )
        }

        val treeIpfsHash = IpfsHash("tree-ipfs-hash")
        val totalAssetAmount = Balance(BigInteger("123"))
        val expectedResult = AssetSnapshot(
            id = assetSnapshotUuid,
            name = name,
            chainId = chainId,
            projectId = PROJECT_ID_1,
            assetContractAddress = assetContractAddress,
            blockNumber = payoutBlock,
            ignoredHolderAddresses = ignoredHolderAddresses,
            data = SuccessfulAssetSnapshotData(
                merkleTreeRootId = treeUuid,
                merkleTreeIpfsHash = treeIpfsHash,
                totalAssetAmount = totalAssetAmount
            )
        )

        verify("asset snapshot is completed") {
            val result = repository.completeAssetSnapshot(
                assetSnapshotId = assetSnapshotUuid,
                merkleTreeRootId = treeUuid,
                merkleTreeIpfsHash = treeIpfsHash,
                totalAssetAmount = totalAssetAmount
            )

            expectThat(result)
                .isEqualTo(expectedResult)
        }

        verify("successful asset snapshot is correctly fetched from database") {
            val result = repository.getById(assetSnapshotUuid)

            expectThat(result)
                .isEqualTo(expectedResult)
        }
    }

    @Test
    fun mustCorrectlyFailAssetSnapshot() {
        val assetSnapshotUuid = AssetSnapshotId(UUID.randomUUID())

        suppose("UUID provider will return specified UUIDs") {
            call(uuidProvider.getUuid(AssetSnapshotId))
                .willReturn(assetSnapshotUuid)
        }

        val name = "asset-snapshot-name"
        val chainId = ChainId(1L)
        val assetContractAddress = ContractAddress("a")
        val payoutBlock = BlockNumber(BigInteger.TEN)
        val ignoredHolderAddresses = setOf(WalletAddress("e"))

        val storedAssetSnapshotId = suppose("asset snapshot is stored into database") {
            repository.createAssetSnapshot(
                CreateAssetSnapshotParams(
                    name = name,
                    chainId = chainId,
                    projectId = PROJECT_ID_1,
                    assetContractAddress = assetContractAddress,
                    payoutBlock = payoutBlock,
                    ignoredHolderAddresses = ignoredHolderAddresses
                )
            )
        }

        verify("correct asset snapshot ID is returned") {
            expectThat(storedAssetSnapshotId)
                .isEqualTo(assetSnapshotUuid)
        }

        val expectedResult = AssetSnapshot(
            id = assetSnapshotUuid,
            name = name,
            chainId = chainId,
            projectId = PROJECT_ID_1,
            assetContractAddress = assetContractAddress,
            blockNumber = payoutBlock,
            ignoredHolderAddresses = ignoredHolderAddresses,
            data = OtherAssetSnapshotData(AssetSnapshotStatus.FAILED, AssetSnapshotFailureCause.OTHER)
        )

        verify("asset snapshot failed") {
            val result = repository.failAssetSnapshot(assetSnapshotUuid, AssetSnapshotFailureCause.OTHER)

            expectThat(result)
                .isEqualTo(expectedResult)
        }

        verify("failed asset snapshot is correctly fetched from database") {
            val result = repository.getById(assetSnapshotUuid)

            expectThat(result)
                .isEqualTo(expectedResult)
        }
    }

    private fun assetSnapshotRecord(
        projectId: ProjectId,
        status: AssetSnapshotStatus,
        failureCause: AssetSnapshotFailureCause?
    ): AssetSnapshotRecord {
        val id = AssetSnapshotId(UUID.randomUUID())
        return AssetSnapshotRecord(
            id = id,
            name = "asset-snapshot-${id.value}",
            chainId = ChainId(1L),
            projectId = projectId,
            assetContractAddress = ContractAddress("a"),
            blockNumber = BlockNumber(BigInteger.TEN),
            ignoredHolderAddresses = emptyArray(),
            status = status,
            failureCause = failureCause,
            resultTree = null,
            treeIpfsHash = null,
            totalAssetAmount = null
        )
    }

    private fun List<AssetSnapshotRecord>.toModels(): List<AssetSnapshot> =
        map {
            AssetSnapshot(
                id = it.id,
                projectId = it.projectId,
                name = it.name,
                chainId = it.chainId,
                assetContractAddress = it.assetContractAddress,
                blockNumber = it.blockNumber,
                ignoredHolderAddresses = emptySet(),
                data = OtherAssetSnapshotData(
                    status = it.status,
                    failureCause = it.failureCause
                )
            )
        }
}
