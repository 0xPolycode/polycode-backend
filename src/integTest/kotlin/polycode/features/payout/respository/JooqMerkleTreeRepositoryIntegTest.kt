package polycode.features.payout.respository

import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import polycode.TestBase
import polycode.config.DatabaseConfig
import polycode.features.payout.model.params.FetchMerkleTreeParams
import polycode.features.payout.model.params.FetchMerkleTreePathParams
import polycode.features.payout.repository.JooqMerkleTreeRepository
import polycode.features.payout.repository.MerkleTreeRepository
import polycode.features.payout.util.HashFunction
import polycode.features.payout.util.MerkleHash
import polycode.features.payout.util.MerkleTree
import polycode.features.payout.util.PayoutAccountBalance
import polycode.generated.jooq.id.MerkleTreeLeafId
import polycode.generated.jooq.id.MerkleTreeRootId
import polycode.generated.jooq.tables.MerkleTreeLeafNodeTable
import polycode.generated.jooq.tables.MerkleTreeRootTable
import polycode.generated.jooq.tables.records.MerkleTreeLeafNodeRecord
import polycode.generated.jooq.tables.records.MerkleTreeRootRecord
import polycode.service.UuidProvider
import polycode.testcontainers.PostgresTestContainer
import polycode.util.Balance
import polycode.util.BlockNumber
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.WalletAddress
import java.math.BigInteger
import java.util.UUID

@JooqTest
@Import(JooqMerkleTreeRepository::class, DatabaseConfig::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqMerkleTreeRepositoryIntegTest : TestBase() {

    @Suppress("unused")
    private val postgresContainer = PostgresTestContainer()

    @Autowired
    private lateinit var repository: MerkleTreeRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @MockBean
    private lateinit var uuidProvider: UuidProvider

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)
    }

    @Test
    fun mustCorrectlyFetchAndReconstructMerkleTreeById() {
        val treeRootUuid = MerkleTreeRootId(UUID.randomUUID())
        val leaf1Uuid = MerkleTreeLeafId(UUID.randomUUID())
        val leaf2Uuid = MerkleTreeLeafId(UUID.randomUUID())
        val leaf3Uuid = MerkleTreeLeafId(UUID.randomUUID())
        val leaf4Uuid = MerkleTreeLeafId(UUID.randomUUID())

        suppose("UUID provider will return specified UUIDs") {
            call(uuidProvider.getUuid(MerkleTreeRootId))
                .willReturn(treeRootUuid)
            call(uuidProvider.getUuid(MerkleTreeLeafId))
                .willReturn(leaf1Uuid, leaf2Uuid, leaf3Uuid, leaf4Uuid)
        }

        val leafNode1 = PayoutAccountBalance(WalletAddress("a"), Balance(BigInteger.ZERO))
        val leafNode2 = PayoutAccountBalance(WalletAddress("b"), Balance(BigInteger("100")))
        val leafNode3 = PayoutAccountBalance(WalletAddress("c"), Balance(BigInteger("200")))
        val leafNode4 = PayoutAccountBalance(WalletAddress("d"), Balance(BigInteger("300")))
        val merkleTree = MerkleTree(listOf(leafNode1, leafNode2, leafNode3, leafNode4), HashFunction.IDENTITY)

        val storedTreeId = suppose("multi-node Merkle tree is stored into database") {
            repository.storeTree(
                tree = merkleTree,
                chainId = ChainId(1L),
                assetContractAddress = ContractAddress("b"),
                blockNumber = BlockNumber(BigInteger("123"))
            )
        }

        verify("correct tree ID is returned") {
            expectThat(storedTreeId)
                .isEqualTo(treeRootUuid)
        }

        verify("Merkle tree is correctly fetched and reconstructed") {
            val result = repository.getById(storedTreeId)

            expectThat(result)
                .isNotNull()
            expectThat(result?.root)
                .isEqualTo(merkleTree.root)
            expectThat(result?.leafNodesByHash)
                .isEqualTo(merkleTree.leafNodesByHash)
            expectThat(result?.hashFn)
                .isEqualTo(merkleTree.hashFn)
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentMerkleTreeById() {
        verify("null is returned when fetching non-existent Merkle tree") {
            val result = repository.getById(MerkleTreeRootId(UUID.randomUUID()))

            expectThat(result)
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyStoreSimpleMerkleTreeIntoDatabase() {
        val treeRootUuid = MerkleTreeRootId(UUID.randomUUID())
        val leafUuid = MerkleTreeLeafId(UUID.randomUUID())

        suppose("UUID provider will return specified UUIDs") {
            call(uuidProvider.getUuid(MerkleTreeRootId))
                .willReturn(treeRootUuid)
            call(uuidProvider.getUuid(MerkleTreeLeafId))
                .willReturn(leafUuid)
        }

        val leafNode = PayoutAccountBalance(WalletAddress("a"), Balance(BigInteger.ZERO))
        val merkleTree = MerkleTree(listOf(leafNode), HashFunction.IDENTITY)

        val chainId = ChainId(1L)
        val contractAddress = ContractAddress("b")
        val storedTreeId = suppose("simple Merkle tree is stored into database") {
            repository.storeTree(
                tree = merkleTree,
                chainId = chainId,
                assetContractAddress = contractAddress,
                blockNumber = BlockNumber(BigInteger("123"))
            )
        }

        verify("correct tree ID is returned") {
            expectThat(storedTreeId)
                .isEqualTo(treeRootUuid)
        }

        verify("simple Merkle tree root is correctly stored into database") {
            val rootRecord = dslContext.selectFrom(MerkleTreeRootTable)
                .where(MerkleTreeRootTable.ID.eq(treeRootUuid))
                .fetchOne()

            expectThat(rootRecord)
                .isEqualTo(
                    MerkleTreeRootRecord(
                        id = treeRootUuid,
                        chainId = chainId,
                        assetContractAddress = contractAddress,
                        blockNumber = BlockNumber(BigInteger("123")),
                        merkleHash = merkleTree.root.hash,
                        hashFn = HashFunction.IDENTITY
                    )
                )
        }

        verify("simple Merkle tree leaf is correctly stored into database") {
            val count = dslContext.selectCount()
                .from(MerkleTreeLeafNodeTable)
                .where(MerkleTreeLeafNodeTable.MERKLE_ROOT.eq(treeRootUuid))
                .fetchOne(DSL.count())

            expectThat(count)
                .isOne()

            val leafRecord = dslContext.selectFrom(MerkleTreeLeafNodeTable)
                .where(MerkleTreeLeafNodeTable.ID.eq(leafUuid))
                .fetchOne()

            expectThat(leafRecord)
                .isEqualTo(
                    MerkleTreeLeafNodeRecord(
                        id = leafUuid,
                        merkleRoot = treeRootUuid,
                        walletAddress = leafNode.address,
                        assetAmount = leafNode.balance
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyStoreMultiNodeMerkleTreeIntoDatabase() {
        val treeRootUuid = MerkleTreeRootId(UUID.randomUUID())
        val leaf1Uuid = MerkleTreeLeafId(UUID.randomUUID())
        val leaf2Uuid = MerkleTreeLeafId(UUID.randomUUID())
        val leaf3Uuid = MerkleTreeLeafId(UUID.randomUUID())
        val leaf4Uuid = MerkleTreeLeafId(UUID.randomUUID())

        suppose("UUID provider will return specified UUIDs") {
            call(uuidProvider.getUuid(MerkleTreeRootId))
                .willReturn(treeRootUuid)
            call(uuidProvider.getUuid(MerkleTreeLeafId))
                .willReturn(leaf1Uuid, leaf2Uuid, leaf3Uuid, leaf4Uuid)
        }

        val leafNode1 = PayoutAccountBalance(WalletAddress("a"), Balance(BigInteger.ZERO))
        val leafNode2 = PayoutAccountBalance(WalletAddress("b"), Balance(BigInteger("100")))
        val leafNode3 = PayoutAccountBalance(WalletAddress("c"), Balance(BigInteger("200")))
        val leafNode4 = PayoutAccountBalance(WalletAddress("d"), Balance(BigInteger("300")))
        val merkleTree = MerkleTree(listOf(leafNode1, leafNode2, leafNode3, leafNode4), HashFunction.IDENTITY)

        val chainId = ChainId(1L)
        val contractAddress = ContractAddress("b")
        val storedTreeId = suppose("multi-node Merkle tree is stored into database") {
            repository.storeTree(
                tree = merkleTree,
                chainId = chainId,
                assetContractAddress = contractAddress,
                blockNumber = BlockNumber(BigInteger("123"))
            )
        }

        verify("correct tree ID is returned") {
            expectThat(storedTreeId)
                .isEqualTo(treeRootUuid)
        }

        verify("multi-node Merkle tree root is correctly stored into database") {
            val rootRecord = dslContext.selectFrom(MerkleTreeRootTable)
                .where(MerkleTreeRootTable.ID.eq(treeRootUuid))
                .fetchOne()

            expectThat(rootRecord)
                .isEqualTo(
                    MerkleTreeRootRecord(
                        id = treeRootUuid,
                        chainId = chainId,
                        assetContractAddress = contractAddress,
                        blockNumber = BlockNumber(BigInteger("123")),
                        merkleHash = merkleTree.root.hash,
                        hashFn = HashFunction.IDENTITY
                    )
                )
        }

        verify("multi-node Merkle tree leaves are correctly stored into database") {
            val count = dslContext.selectCount()
                .from(MerkleTreeLeafNodeTable)
                .where(MerkleTreeLeafNodeTable.MERKLE_ROOT.eq(treeRootUuid))
                .fetchOne(DSL.count())

            expectThat(count)
                .isEqualTo(4)

            val leaf1Record = dslContext.selectFrom(MerkleTreeLeafNodeTable)
                .where(MerkleTreeLeafNodeTable.ID.eq(leaf1Uuid))
                .fetchOne()

            expectThat(leaf1Record)
                .isEqualTo(
                    MerkleTreeLeafNodeRecord(
                        id = leaf1Uuid,
                        merkleRoot = treeRootUuid,
                        walletAddress = leafNode1.address,
                        assetAmount = leafNode1.balance
                    )
                )

            val leaf2Record = dslContext.selectFrom(MerkleTreeLeafNodeTable)
                .where(MerkleTreeLeafNodeTable.ID.eq(leaf2Uuid))
                .fetchOne()

            expectThat(leaf2Record)
                .isEqualTo(
                    MerkleTreeLeafNodeRecord(
                        id = leaf2Uuid,
                        merkleRoot = treeRootUuid,
                        walletAddress = leafNode2.address,
                        assetAmount = leafNode2.balance
                    )
                )

            val leaf3Record = dslContext.selectFrom(MerkleTreeLeafNodeTable)
                .where(MerkleTreeLeafNodeTable.ID.eq(leaf3Uuid))
                .fetchOne()

            expectThat(leaf3Record)
                .isEqualTo(
                    MerkleTreeLeafNodeRecord(
                        id = leaf3Uuid,
                        merkleRoot = treeRootUuid,
                        walletAddress = leafNode3.address,
                        assetAmount = leafNode3.balance
                    )
                )

            val leaf4Record = dslContext.selectFrom(MerkleTreeLeafNodeTable)
                .where(MerkleTreeLeafNodeTable.ID.eq(leaf4Uuid))
                .fetchOne()

            expectThat(leaf4Record)
                .isEqualTo(
                    MerkleTreeLeafNodeRecord(
                        id = leaf4Uuid,
                        merkleRoot = treeRootUuid,
                        walletAddress = leafNode4.address,
                        assetAmount = leafNode4.balance
                    )
                )
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentMerkleTreeByHash() {
        verify("null is returned when fetching non-existent Merkle tree") {
            val result = repository.fetchTree(
                FetchMerkleTreeParams(
                    rootHash = MerkleHash("a"),
                    chainId = ChainId(1L),
                    assetContractAddress = ContractAddress("1")
                )
            )

            expectThat(result)
                .isNull()
        }
    }

    @Test
    fun mustReturnNullWhenMerkleTreeReconstructionFailsDuringFetchByHash() {
        val treeRootUuid = MerkleTreeRootId(UUID.randomUUID())
        val leaf1Uuid = MerkleTreeLeafId(UUID.randomUUID())
        val leaf2Uuid = MerkleTreeLeafId(UUID.randomUUID())
        val leaf3Uuid = MerkleTreeLeafId(UUID.randomUUID())
        val leaf4Uuid = MerkleTreeLeafId(UUID.randomUUID())

        suppose("UUID provider will return specified UUIDs") {
            call(uuidProvider.getUuid(MerkleTreeRootId))
                .willReturn(treeRootUuid)
            call(uuidProvider.getUuid(MerkleTreeLeafId))
                .willReturn(leaf1Uuid, leaf2Uuid, leaf3Uuid, leaf4Uuid)
        }

        val leafNode1 = PayoutAccountBalance(WalletAddress("a"), Balance(BigInteger.ZERO))
        val leafNode2 = PayoutAccountBalance(WalletAddress("b"), Balance(BigInteger("100")))
        val leafNode3 = PayoutAccountBalance(WalletAddress("c"), Balance(BigInteger("200")))
        val leafNode4 = PayoutAccountBalance(WalletAddress("d"), Balance(BigInteger("300")))
        val merkleTree = MerkleTree(listOf(leafNode1, leafNode2, leafNode3, leafNode4), HashFunction.IDENTITY)

        val storedTreeId = suppose("multi-node Merkle tree is stored into database") {
            repository.storeTree(
                tree = merkleTree,
                chainId = ChainId(1L),
                assetContractAddress = ContractAddress("b"),
                blockNumber = BlockNumber(BigInteger("123"))
            )
        }

        verify("correct tree ID is returned") {
            expectThat(storedTreeId)
                .isEqualTo(treeRootUuid)
        }

        suppose("Merkle tree leaf node was deleted without updating root hash") {
            dslContext.deleteFrom(MerkleTreeLeafNodeTable)
                .where(MerkleTreeLeafNodeTable.ID.eq(leaf4Uuid))
                .execute()
        }

        verify("null is returned when fetching Merkle tree") {
            val result = repository.fetchTree(
                FetchMerkleTreeParams(
                    rootHash = merkleTree.root.hash,
                    chainId = ChainId(1L),
                    assetContractAddress = ContractAddress("b")
                )
            )

            expectThat(result)
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyFetchAndReconstructMerkleTreeByHash() {
        val treeRootUuid = MerkleTreeRootId(UUID.randomUUID())
        val leaf1Uuid = MerkleTreeLeafId(UUID.randomUUID())
        val leaf2Uuid = MerkleTreeLeafId(UUID.randomUUID())
        val leaf3Uuid = MerkleTreeLeafId(UUID.randomUUID())
        val leaf4Uuid = MerkleTreeLeafId(UUID.randomUUID())

        suppose("UUID provider will return specified UUIDs") {
            call(uuidProvider.getUuid(MerkleTreeRootId))
                .willReturn(treeRootUuid)
            call(uuidProvider.getUuid(MerkleTreeLeafId))
                .willReturn(leaf1Uuid, leaf2Uuid, leaf3Uuid, leaf4Uuid)
        }

        val leafNode1 = PayoutAccountBalance(WalletAddress("a"), Balance(BigInteger.ZERO))
        val leafNode2 = PayoutAccountBalance(WalletAddress("b"), Balance(BigInteger("100")))
        val leafNode3 = PayoutAccountBalance(WalletAddress("c"), Balance(BigInteger("200")))
        val leafNode4 = PayoutAccountBalance(WalletAddress("d"), Balance(BigInteger("300")))
        val merkleTree = MerkleTree(listOf(leafNode1, leafNode2, leafNode3, leafNode4), HashFunction.IDENTITY)

        val storedTreeId = suppose("multi-node Merkle tree is stored into database") {
            repository.storeTree(
                tree = merkleTree,
                chainId = ChainId(1L),
                assetContractAddress = ContractAddress("b"),
                blockNumber = BlockNumber(BigInteger("123"))
            )
        }

        verify("correct tree ID is returned") {
            expectThat(storedTreeId)
                .isEqualTo(treeRootUuid)
        }

        verify("Merkle tree is correctly fetched and reconstructed") {
            val result = repository.fetchTree(
                FetchMerkleTreeParams(
                    rootHash = merkleTree.root.hash,
                    chainId = ChainId(1L),
                    assetContractAddress = ContractAddress("b")
                )
            )

            expectThat(result)
                .isNotNull()
            expectThat(result?.treeId)
                .isEqualTo(treeRootUuid)
            expectThat(result?.tree?.root)
                .isEqualTo(merkleTree.root)
            expectThat(result?.tree?.leafNodesByHash)
                .isEqualTo(merkleTree.leafNodesByHash)
            expectThat(result?.tree?.hashFn)
                .isEqualTo(merkleTree.hashFn)
        }
    }

    @Test
    fun mustCorrectlyCheckIfLeafNodeExists() {
        val treeRootUuid = MerkleTreeRootId(UUID.randomUUID())
        val leaf1Uuid = MerkleTreeLeafId(UUID.randomUUID())
        val leaf2Uuid = MerkleTreeLeafId(UUID.randomUUID())
        val leaf3Uuid = MerkleTreeLeafId(UUID.randomUUID())
        val leaf4Uuid = MerkleTreeLeafId(UUID.randomUUID())

        suppose("UUID provider will return specified UUIDs") {
            call(uuidProvider.getUuid(MerkleTreeRootId))
                .willReturn(treeRootUuid)
            call(uuidProvider.getUuid(MerkleTreeLeafId))
                .willReturn(leaf1Uuid, leaf2Uuid, leaf3Uuid, leaf4Uuid)
        }

        val leafNode1 = PayoutAccountBalance(WalletAddress("a"), Balance(BigInteger.ZERO))
        val leafNode2 = PayoutAccountBalance(WalletAddress("b"), Balance(BigInteger("100")))
        val leafNode3 = PayoutAccountBalance(WalletAddress("c"), Balance(BigInteger("200")))
        val leafNode4 = PayoutAccountBalance(WalletAddress("d"), Balance(BigInteger("300")))
        val merkleTree = MerkleTree(listOf(leafNode1, leafNode2, leafNode3, leafNode4), HashFunction.IDENTITY)

        val storedTreeId = suppose("multi-node Merkle tree is stored into database") {
            repository.storeTree(
                tree = merkleTree,
                chainId = ChainId(1L),
                assetContractAddress = ContractAddress("b"),
                blockNumber = BlockNumber(BigInteger("123"))
            )
        }

        verify("correct tree ID is returned") {
            expectThat(storedTreeId)
                .isEqualTo(treeRootUuid)
        }

        verify("multi-node Merkle tree leaves are correctly contained within the tree") {
            val leaf1Result = repository.containsAddress(
                FetchMerkleTreePathParams(
                    rootHash = merkleTree.root.hash,
                    chainId = ChainId(1L),
                    assetContractAddress = ContractAddress("b"),
                    walletAddress = leafNode1.address
                )
            )

            expectThat(leaf1Result)
                .isTrue()

            val leaf2Result = repository.containsAddress(
                FetchMerkleTreePathParams(
                    rootHash = merkleTree.root.hash,
                    chainId = ChainId(1L),
                    assetContractAddress = ContractAddress("b"),
                    walletAddress = leafNode2.address
                )
            )

            expectThat(leaf2Result)
                .isTrue()

            val leaf3Result = repository.containsAddress(
                FetchMerkleTreePathParams(
                    rootHash = merkleTree.root.hash,
                    chainId = ChainId(1L),
                    assetContractAddress = ContractAddress("b"),
                    walletAddress = leafNode3.address
                )
            )

            expectThat(leaf3Result)
                .isTrue()

            val leaf4Result = repository.containsAddress(
                FetchMerkleTreePathParams(
                    rootHash = merkleTree.root.hash,
                    chainId = ChainId(1L),
                    assetContractAddress = ContractAddress("b"),
                    walletAddress = leafNode4.address
                )
            )

            expectThat(leaf4Result)
                .isTrue()
        }

        verify("other leaves are not contained within the tree") {
            val fakeLeaf1Result = repository.containsAddress(
                FetchMerkleTreePathParams(
                    rootHash = merkleTree.root.hash,
                    chainId = ChainId(2L),
                    assetContractAddress = ContractAddress("b"),
                    walletAddress = leafNode1.address
                )
            )

            expectThat(fakeLeaf1Result)
                .isFalse()

            val fakeLeaf2Result = repository.containsAddress(
                FetchMerkleTreePathParams(
                    rootHash = merkleTree.root.hash,
                    chainId = ChainId(1L),
                    assetContractAddress = ContractAddress("c"),
                    walletAddress = leafNode1.address
                )
            )

            expectThat(fakeLeaf2Result)
                .isFalse()

            val fakeLeaf3Result = repository.containsAddress(
                FetchMerkleTreePathParams(
                    rootHash = merkleTree.root.hash,
                    chainId = ChainId(1L),
                    assetContractAddress = ContractAddress("b"),
                    walletAddress = WalletAddress("5")
                )
            )

            expectThat(fakeLeaf3Result)
                .isFalse()
        }
    }
}
