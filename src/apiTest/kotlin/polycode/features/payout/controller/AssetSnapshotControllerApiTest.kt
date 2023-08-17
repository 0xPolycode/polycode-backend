package polycode.features.payout.controller

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import org.jooq.DSLContext
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.methods.request.EthFilter
import org.web3j.tx.gas.DefaultGasProvider
import polycode.ControllerTestBase
import polycode.TestData
import polycode.blockchain.SimpleERC20
import polycode.config.CustomHeaders
import polycode.config.TestSchedulerConfiguration
import polycode.features.api.access.model.result.Project
import polycode.features.payout.model.params.FetchMerkleTreeParams
import polycode.features.payout.model.response.AssetSnapshotResponse
import polycode.features.payout.model.response.AssetSnapshotsResponse
import polycode.features.payout.model.response.CreateAssetSnapshotResponse
import polycode.features.payout.model.result.AssetSnapshot
import polycode.features.payout.model.result.FullAssetSnapshot
import polycode.features.payout.model.result.FullAssetSnapshotData
import polycode.features.payout.model.result.OtherAssetSnapshotData
import polycode.features.payout.repository.AssetSnapshotRepository
import polycode.features.payout.repository.MerkleTreeRepository
import polycode.features.payout.service.AssetSnapshotQueueService
import polycode.features.payout.service.ManualFixedScheduler
import polycode.features.payout.util.AssetSnapshotStatus
import polycode.features.payout.util.HashFunction
import polycode.features.payout.util.IpfsHash
import polycode.features.payout.util.MerkleHash
import polycode.generated.jooq.enums.UserIdentifierType
import polycode.generated.jooq.id.ApiKeyId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import polycode.generated.jooq.tables.records.ApiKeyRecord
import polycode.generated.jooq.tables.records.ProjectRecord
import polycode.generated.jooq.tables.records.UserIdentifierRecord
import polycode.testcontainers.HardhatTestContainer
import polycode.util.Balance
import polycode.util.BaseUrl
import polycode.util.BlockNumber
import polycode.util.ContractAddress
import polycode.util.WalletAddress
import polycode.wiremock.WireMock
import java.math.BigInteger
import java.util.UUID

@Import(TestSchedulerConfiguration::class)
class AssetSnapshotControllerApiTest : ControllerTestBase() {

    companion object {
        private val PROJECT_ID = ProjectId(UUID.randomUUID())
        private val OWNER_ID = UserId(UUID.randomUUID())
        private val PROJECT = Project(
            id = PROJECT_ID,
            ownerId = OWNER_ID,
            baseRedirectUrl = BaseUrl("https://example.com/"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = null,
            createdAt = TestData.TIMESTAMP
        )
        private const val API_KEY = "api-key"
    }

    private val accounts = HardhatTestContainer.ACCOUNTS

    @Autowired
    private lateinit var merkleTreeRepository: MerkleTreeRepository

    @Autowired
    private lateinit var snapshotRepository: AssetSnapshotRepository

    @Autowired
    private lateinit var snapshotQueueService: AssetSnapshotQueueService

    @Autowired
    private lateinit var dslContext: DSLContext

    @Autowired
    private lateinit var assetSnapshotQueueScheduler: ManualFixedScheduler

    @BeforeAll
    fun beforeAll() {
        WireMock.start()
    }

    @AfterAll
    fun afterAll() {
        WireMock.stop()
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
                id = PROJECT.id,
                ownerId = PROJECT.ownerId,
                baseRedirectUrl = PROJECT.baseRedirectUrl,
                chainId = PROJECT.chainId,
                customRpcUrl = PROJECT.customRpcUrl,
                createdAt = PROJECT.createdAt
            )
        )

        dslContext.executeInsert(
            ApiKeyRecord(
                id = ApiKeyId(UUID.randomUUID()),
                projectId = PROJECT_ID,
                apiKey = API_KEY,
                createdAt = TestData.TIMESTAMP
            )
        )

        WireMock.reset()
    }

    @Test
    fun mustSuccessfullyCreateAssetSnapshotForSomeAsset() {
        val mainAccount = accounts[0]

        val contract = suppose("simple ERC20 contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(mainAccount.address),
                listOf(BigInteger("10000")),
                mainAccount.address
            ).send()
        }

        suppose("some accounts get ERC20 tokens") {
            contract.transfer(accounts[1].address, BigInteger("100"))
            contract.transfer(accounts[2].address, BigInteger("200"))
            contract.transfer(accounts[3].address, BigInteger("300"))
            contract.transfer(accounts[4].address, BigInteger("400"))
            hardhatContainer.mine()
        }

        val payoutBlock = hardhatContainer.blockNumber()

        contract.applyWeb3jFilterFix(BlockNumber(BigInteger.ZERO), payoutBlock)

        suppose("some additional transactions of ERC20 token are made") {
            contract.transfer(accounts[1].address, BigInteger("900"))
            contract.transfer(accounts[5].address, BigInteger("1000"))
            contract.transfer(accounts[6].address, BigInteger("2000"))
            hardhatContainer.mine()
        }

        val ipfsHash = IpfsHash("test-hash")

        suppose("Merkle tree will be stored to IPFS") {
            WireMock.server.stubFor(
                post(urlPathEqualTo("/pinning/pinJSONToIPFS"))
                    .withHeader("pinata_api_key", equalTo("test-api-key"))
                    .withHeader("pinata_secret_api_key", equalTo("test-api-secret"))
                    .willReturn(
                        aResponse()
                            .withBody(
                                """
                                {
                                    "IpfsHash": "${ipfsHash.value}",
                                    "PinSize": 1,
                                    "Timestamp": "2022-01-01T00:00:00Z"
                                }
                                """.trimIndent()
                            )
                            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                            .withStatus(200)
                    )
            )
        }

        val ignoredAddresses = setOf(mainAccount.address, accounts[4].address)

        val name = "asset-snapshot-name"
        val createResponse = suppose("create asset snapshot request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/asset-snapshots")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\n    \"name\": \"$name\",\n" +
                            "   \"asset_address\": \"${contract.contractAddress}\",\n " +
                            "   \"payout_block_number\": \"${payoutBlock.value}\",\n " +
                            "   \"ignored_holder_addresses\": ${ignoredAddresses.map { "\"$it\"" }}\n}"
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, CreateAssetSnapshotResponse::class.java)
        }

        verify("asset snapshot is created in database") {
            val result = snapshotRepository.getById(createResponse.id)

            expectThat(result)
                .isNotNull()
            expectThat(result)
                .isEqualTo(
                    AssetSnapshot(
                        id = createResponse.id,
                        name = name,
                        chainId = TestData.CHAIN_ID,
                        projectId = PROJECT_ID,
                        assetContractAddress = ContractAddress(contract.contractAddress),
                        blockNumber = payoutBlock,
                        ignoredHolderAddresses = ignoredAddresses.mapTo(HashSet()) { WalletAddress(it) },
                        data = OtherAssetSnapshotData(AssetSnapshotStatus.PENDING, null)
                    )
                )
        }
    }

    @Test
    fun mustSuccessfullyCreateAndProcessAssetSnapshotForSomeAsset() {
        val mainAccount = accounts[0]

        val contract = suppose("simple ERC20 contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(mainAccount.address),
                listOf(BigInteger("10000")),
                mainAccount.address
            ).send()
        }

        suppose("some accounts get ERC20 tokens") {
            contract.transfer(accounts[1].address, BigInteger("100")).send()
            contract.transfer(accounts[2].address, BigInteger("200")).send()
            contract.transfer(accounts[3].address, BigInteger("300")).send()
            contract.transfer(accounts[4].address, BigInteger("400")).send()
            hardhatContainer.mine()
        }

        val payoutBlock = hardhatContainer.blockNumber()

        contract.applyWeb3jFilterFix(BlockNumber(BigInteger.ZERO), payoutBlock)

        suppose("some additional transactions of ERC20 token are made") {
            contract.transfer(accounts[1].address, BigInteger("900")).send()
            contract.transfer(accounts[5].address, BigInteger("1000")).send()
            contract.transfer(accounts[6].address, BigInteger("2000")).send()
            hardhatContainer.mine()
        }

        val ipfsHash = IpfsHash("test-hash")

        suppose("Merkle tree will be stored to IPFS") {
            WireMock.server.stubFor(
                post(urlPathEqualTo("/pinning/pinJSONToIPFS"))
                    .withHeader("pinata_api_key", equalTo("test-api-key"))
                    .withHeader("pinata_secret_api_key", equalTo("test-api-secret"))
                    .willReturn(
                        aResponse()
                            .withBody(
                                """
                                {
                                    "IpfsHash": "${ipfsHash.value}",
                                    "PinSize": 1,
                                    "Timestamp": "2022-01-01T00:00:00Z"
                                }
                                """.trimIndent()
                            )
                            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                            .withStatus(200)
                    )
            )
        }

        val ignoredAddresses = setOf(mainAccount.address, accounts[4].address)

        val name = "asset-snapshot-name"
        val createResponse = suppose("create asset snapshot request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/asset-snapshots")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\n    \"name\": \"$name\",\n" +
                            "   \"asset_address\": \"${contract.contractAddress}\",\n " +
                            "   \"payout_block_number\": \"${payoutBlock.value}\",\n " +
                            "   \"ignored_holder_addresses\": ${ignoredAddresses.map { "\"$it\"" }}\n}"
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, CreateAssetSnapshotResponse::class.java)
        }

        val pendingSnapshot = suppose("asset snapshot is fetched by ID before execution") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/asset-snapshots/${createResponse.id.value}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, AssetSnapshotResponse::class.java)
        }

        verify("pending asset snapshot has correct payload") {
            expectThat(pendingSnapshot)
                .isEqualTo(
                    FullAssetSnapshot(
                        id = createResponse.id,
                        name = name,
                        chainId = TestData.CHAIN_ID,
                        projectId = PROJECT_ID,
                        assetContractAddress = ContractAddress(contract.contractAddress),
                        blockNumber = payoutBlock,
                        ignoredHolderAddresses = ignoredAddresses.mapTo(HashSet()) { WalletAddress(it) },
                        snapshotStatus = AssetSnapshotStatus.PENDING,
                        snapshotFailureCause = null,
                        data = null
                    ).toAssetSnapshotResponse()
                )
        }

        suppose("asset snapshot is processed") {
            assetSnapshotQueueScheduler.execute()
        }

        val completedSnapshot = suppose("asset snapshot is fetched by ID after execution") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/asset-snapshots/${createResponse.id.value}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, AssetSnapshotResponse::class.java)
        }

        verify("completed asset snapshot has correct payload") {
            expectThat(completedSnapshot)
                .isEqualTo(
                    FullAssetSnapshot(
                        id = createResponse.id,
                        name = name,
                        chainId = TestData.CHAIN_ID,
                        projectId = PROJECT_ID,
                        assetContractAddress = ContractAddress(contract.contractAddress),
                        blockNumber = payoutBlock,
                        ignoredHolderAddresses = ignoredAddresses.mapTo(HashSet()) { WalletAddress(it) },
                        snapshotStatus = AssetSnapshotStatus.SUCCESS,
                        snapshotFailureCause = null,
                        data = FullAssetSnapshotData(
                            totalAssetAmount = Balance(BigInteger("600")),
                            // checked in next verify block
                            merkleRootHash = MerkleHash(completedSnapshot.assetSnapshotMerkleRoot!!),
                            merkleTreeIpfsHash = ipfsHash,
                            // checked in next verify block
                            merkleTreeDepth = completedSnapshot.assetSnapshotMerkleDepth!!,
                            hashFn = HashFunction.KECCAK_256
                        )
                    ).toAssetSnapshotResponse()
                )
        }

        verify("Merkle tree is correctly created in the database") {
            val result = merkleTreeRepository.fetchTree(
                FetchMerkleTreeParams(
                    rootHash = MerkleHash(completedSnapshot.assetSnapshotMerkleRoot!!),
                    chainId = TestData.CHAIN_ID,
                    assetContractAddress = ContractAddress(contract.contractAddress)
                )
            )

            expectThat(result)
                .isNotNull()

            expectThat(result?.tree?.leafNodesByAddress)
                .hasSize(3)
            expectThat(result?.tree?.leafNodesByAddress?.keys)
                .containsExactlyInAnyOrder(
                    WalletAddress(accounts[1].address),
                    WalletAddress(accounts[2].address),
                    WalletAddress(accounts[3].address)
                )
            expectThat(result?.tree?.leafNodesByAddress?.get(WalletAddress(accounts[1].address))?.value?.data?.balance)
                .isEqualTo(Balance(BigInteger("100")))
            expectThat(result?.tree?.leafNodesByAddress?.get(WalletAddress(accounts[2].address))?.value?.data?.balance)
                .isEqualTo(Balance(BigInteger("200")))
            expectThat(result?.tree?.leafNodesByAddress?.get(WalletAddress(accounts[3].address))?.value?.data?.balance)
                .isEqualTo(Balance(BigInteger("300")))

            expectThat(completedSnapshot.assetSnapshotMerkleDepth)
                .isEqualTo(result?.tree?.root?.depth)
        }
    }

    @Test
    fun mustReturnAssetSnapshotsForSomeProjectId() {
        val mainAccount = accounts[0]

        val erc20Contract = suppose("simple ERC20 contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(mainAccount.address),
                listOf(BigInteger("10000")),
                mainAccount.address
            ).send()
        }

        suppose("some accounts get ERC20 tokens") {
            erc20Contract.transfer(accounts[1].address, BigInteger("100"))
            erc20Contract.transfer(accounts[2].address, BigInteger("200"))
            erc20Contract.transfer(accounts[3].address, BigInteger("300"))
            erc20Contract.transfer(accounts[4].address, BigInteger("400"))
            hardhatContainer.mine()
        }

        val payoutBlock = hardhatContainer.blockNumber()

        erc20Contract.applyWeb3jFilterFix(BlockNumber(BigInteger.ZERO), payoutBlock)

        suppose("some additional transactions of ERC20 token are made") {
            erc20Contract.transfer(accounts[1].address, BigInteger("900"))
            erc20Contract.transfer(accounts[5].address, BigInteger("1000"))
            erc20Contract.transfer(accounts[6].address, BigInteger("2000"))
            hardhatContainer.mine()
        }

        val ipfsHash = IpfsHash("test-hash")

        suppose("Merkle tree will be stored to IPFS") {
            WireMock.server.stubFor(
                post(urlPathEqualTo("/pinning/pinJSONToIPFS"))
                    .withHeader("pinata_api_key", equalTo("test-api-key"))
                    .withHeader("pinata_secret_api_key", equalTo("test-api-secret"))
                    .willReturn(
                        aResponse()
                            .withBody(
                                """
                                {
                                    "IpfsHash": "${ipfsHash.value}",
                                    "PinSize": 1,
                                    "Timestamp": "2022-01-01T00:00:00Z"
                                }
                                """.trimIndent()
                            )
                            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                            .withStatus(200)
                    )
            )
        }

        val ignoredAddresses = setOf(mainAccount.address, accounts[4].address)

        val name = "asset-snapshot-name"
        val createResponse = suppose("create asset snapshot request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/asset-snapshots")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\n    \"name\": \"$name\",\n" +
                            "   \"asset_address\": \"${erc20Contract.contractAddress}\",\n " +
                            "   \"payout_block_number\": \"${payoutBlock.value}\",\n " +
                            "   \"ignored_holder_addresses\": ${ignoredAddresses.map { "\"$it\"" }}\n}"
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, CreateAssetSnapshotResponse::class.java)
        }

        suppose("asset snapshot is processed") {
            assetSnapshotQueueScheduler.execute()
        }

        val assetSnapshot = snapshotQueueService.getAssetSnapshotById(createResponse.id)!!

        val statusesString = AssetSnapshotStatus.values().joinToString(separator = ",") { it.name }
        val adminPayouts = suppose("asset snapshots are fetched for project") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/asset-snapshots/by-project/${PROJECT_ID.value}?status=$statusesString")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, AssetSnapshotsResponse::class.java)
        }

        verify("correct asset snapshots are returned") {
            expectThat(adminPayouts)
                .isEqualTo(AssetSnapshotsResponse(listOf(assetSnapshot.toAssetSnapshotResponse())))
        }
    }

    @Test
    fun mustBeAbleToCreateAndProcessSameAssetSnapshotTwiceAndGetTheSameResponse() {
        val mainAccount = accounts[0]

        val contract = suppose("simple ERC20 contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(mainAccount.address),
                listOf(BigInteger("10000")),
                mainAccount.address
            ).send()
        }

        suppose("some accounts get ERC20 tokens") {
            contract.transfer(accounts[1].address, BigInteger("100")).send()
            contract.transfer(accounts[2].address, BigInteger("200")).send()
            contract.transfer(accounts[3].address, BigInteger("300")).send()
            contract.transfer(accounts[4].address, BigInteger("400")).send()
            hardhatContainer.mine()
        }

        val payoutBlock = hardhatContainer.blockNumber()

        contract.applyWeb3jFilterFix(BlockNumber(BigInteger.ZERO), payoutBlock)

        suppose("some additional transactions of ERC20 token are made") {
            contract.transfer(accounts[1].address, BigInteger("900")).send()
            contract.transfer(accounts[5].address, BigInteger("1000")).send()
            contract.transfer(accounts[6].address, BigInteger("2000")).send()
            hardhatContainer.mine()
        }

        val ipfsHash = IpfsHash("test-hash")

        suppose("Merkle tree will be stored to IPFS") {
            WireMock.server.stubFor(
                post(urlPathEqualTo("/pinning/pinJSONToIPFS"))
                    .withHeader("pinata_api_key", equalTo("test-api-key"))
                    .withHeader("pinata_secret_api_key", equalTo("test-api-secret"))
                    .willReturn(
                        aResponse()
                            .withBody(
                                """
                                {
                                    "IpfsHash": "${ipfsHash.value}",
                                    "PinSize": 1,
                                    "Timestamp": "2022-01-01T00:00:00Z"
                                }
                                """.trimIndent()
                            )
                            .withHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                            .withStatus(200)
                    )
            )
        }

        val name1 = "asset-snapshot-name-1"
        val createResponse = suppose("first create asset snapshot request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/asset-snapshots")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\n    \"name\": \"$name1\",\n" +
                            "   \"asset_address\": \"${contract.contractAddress}\",\n " +
                            "   \"payout_block_number\": \"${payoutBlock.value}\",\n " +
                            "   \"ignored_holder_addresses\": []\n}"
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, CreateAssetSnapshotResponse::class.java)
        }

        val pendingAssetSnapshot = suppose("first asset snapshot is fetched by ID before processing") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/asset-snapshots/${createResponse.id.value}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, AssetSnapshotResponse::class.java)
        }

        verify("first pending asset snapshot has correct payload") {
            expectThat(pendingAssetSnapshot)
                .isEqualTo(
                    FullAssetSnapshot(
                        id = createResponse.id,
                        name = name1,
                        chainId = TestData.CHAIN_ID,
                        projectId = PROJECT_ID,
                        assetContractAddress = ContractAddress(contract.contractAddress),
                        blockNumber = payoutBlock,
                        ignoredHolderAddresses = emptySet(),
                        snapshotStatus = AssetSnapshotStatus.PENDING,
                        snapshotFailureCause = null,
                        data = null
                    ).toAssetSnapshotResponse()
                )
        }

        suppose("first asset snapshot is processed") {
            assetSnapshotQueueScheduler.execute()
        }

        val completedSnapshot = suppose("first asset snapshot is fetched by ID after processing") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/asset-snapshots/${createResponse.id.value}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, AssetSnapshotResponse::class.java)
        }

        verify("first asset completed snapshot has correct payload") {
            expectThat(completedSnapshot)
                .isEqualTo(
                    FullAssetSnapshot(
                        id = createResponse.id,
                        name = name1,
                        chainId = TestData.CHAIN_ID,
                        projectId = PROJECT_ID,
                        assetContractAddress = ContractAddress(contract.contractAddress),
                        blockNumber = payoutBlock,
                        ignoredHolderAddresses = emptySet(),
                        snapshotStatus = AssetSnapshotStatus.SUCCESS,
                        snapshotFailureCause = null,
                        data = FullAssetSnapshotData(
                            totalAssetAmount = Balance(BigInteger("10000")),
                            // checked in next verify block
                            merkleRootHash = MerkleHash(completedSnapshot.assetSnapshotMerkleRoot!!),
                            merkleTreeIpfsHash = ipfsHash,
                            // checked in next verify block
                            merkleTreeDepth = completedSnapshot.assetSnapshotMerkleDepth!!,
                            hashFn = HashFunction.KECCAK_256
                        )
                    ).toAssetSnapshotResponse()
                )
        }

        verify("Merkle tree is correctly created in the database") {
            val result = merkleTreeRepository.fetchTree(
                FetchMerkleTreeParams(
                    rootHash = MerkleHash(completedSnapshot.assetSnapshotMerkleRoot!!),
                    chainId = TestData.CHAIN_ID,
                    assetContractAddress = ContractAddress(contract.contractAddress)
                )
            )

            expectThat(result)
                .isNotNull()

            expectThat(result?.tree?.leafNodesByAddress)
                .hasSize(5)
            expectThat(result?.tree?.leafNodesByAddress?.keys)
                .containsExactlyInAnyOrder(
                    WalletAddress(mainAccount.address),
                    WalletAddress(accounts[1].address),
                    WalletAddress(accounts[2].address),
                    WalletAddress(accounts[3].address),
                    WalletAddress(accounts[4].address)
                )
            expectThat(result?.tree?.leafNodesByAddress?.get(WalletAddress(mainAccount.address))?.value?.data?.balance)
                .isEqualTo(Balance(BigInteger("9000")))
            expectThat(result?.tree?.leafNodesByAddress?.get(WalletAddress(accounts[1].address))?.value?.data?.balance)
                .isEqualTo(Balance(BigInteger("100")))
            expectThat(result?.tree?.leafNodesByAddress?.get(WalletAddress(accounts[2].address))?.value?.data?.balance)
                .isEqualTo(Balance(BigInteger("200")))
            expectThat(result?.tree?.leafNodesByAddress?.get(WalletAddress(accounts[3].address))?.value?.data?.balance)
                .isEqualTo(Balance(BigInteger("300")))
            expectThat(result?.tree?.leafNodesByAddress?.get(WalletAddress(accounts[4].address))?.value?.data?.balance)
                .isEqualTo(Balance(BigInteger("400")))

            expectThat(completedSnapshot.assetSnapshotMerkleDepth)
                .isEqualTo(result?.tree?.root?.depth)
        }

        val name2 = "asset-snapshot-name-2"
        val secondCreateResponse = suppose("second create asset snapshot request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/asset-snapshots")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\n    \"name\": \"$name2\",\n" +
                            "   \"asset_address\": \"${contract.contractAddress}\",\n " +
                            "   \"payout_block_number\": \"${payoutBlock.value}\",\n " +
                            "   \"ignored_holder_addresses\": []\n}"
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, CreateAssetSnapshotResponse::class.java)
        }

        suppose("second asset snapshot is processed") {
            assetSnapshotQueueScheduler.execute()
        }

        val secondCompletedSnapshot = suppose("second asset snapshot is fetched by ID after processing") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/asset-snapshots/${secondCreateResponse.id.value}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, AssetSnapshotResponse::class.java)
        }

        verify("second completed asset snapshot is has correct payload") {
            expectThat(secondCompletedSnapshot)
                .isEqualTo(completedSnapshot.copy(id = secondCreateResponse.id, name = name2))
        }
    }

    // This is needed to make web3j work correctly with Hardhat until https://github.com/web3j/web3j/pull/1580 is merged
    private fun SimpleERC20.applyWeb3jFilterFix(startBlock: BlockNumber?, endBlock: BlockNumber) {
        val startBlockParameter =
            startBlock?.value?.let(DefaultBlockParameter::valueOf) ?: DefaultBlockParameterName.EARLIEST
        val endBlockParameter = DefaultBlockParameter.valueOf(endBlock.value)

        repeat(15) {
            hardhatContainer.web3j.ethNewFilter(
                EthFilter(startBlockParameter, endBlockParameter, contractAddress)
            ).send()
        }
    }
}
