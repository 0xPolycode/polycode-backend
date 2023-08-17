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
import org.web3j.utils.Numeric
import polycode.ControllerTestBase
import polycode.TestData
import polycode.blockchain.PayoutStruct
import polycode.blockchain.SimpleERC20
import polycode.blockchain.SimplePayoutManager
import polycode.config.CustomHeaders
import polycode.config.TestSchedulerConfiguration
import polycode.features.api.access.model.result.Project
import polycode.features.payout.model.response.CreateAssetSnapshotResponse
import polycode.features.payout.model.response.InvestorPayoutResponse
import polycode.features.payout.model.response.InvestorPayoutsResponse
import polycode.features.payout.model.response.PayoutResponse
import polycode.features.payout.service.AssetSnapshotQueueService
import polycode.features.payout.service.ManualFixedScheduler
import polycode.features.payout.util.IpfsHash
import polycode.generated.jooq.enums.UserIdentifierType
import polycode.generated.jooq.id.ApiKeyId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import polycode.generated.jooq.tables.records.ApiKeyRecord
import polycode.generated.jooq.tables.records.ProjectRecord
import polycode.generated.jooq.tables.records.UserIdentifierRecord
import polycode.security.WithMockUser
import polycode.testcontainers.HardhatTestContainer
import polycode.util.BaseUrl
import polycode.util.BlockNumber
import polycode.util.ContractAddress
import polycode.util.WalletAddress
import polycode.wiremock.WireMock
import java.math.BigInteger
import java.util.UUID

@Import(TestSchedulerConfiguration::class)
class ClaimablePayoutsControllerApiTest : ControllerTestBase() {

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
    private lateinit var snapshotQueueService: AssetSnapshotQueueService

    @Autowired
    private lateinit var dslContext: DSLContext

    @Autowired
    private lateinit var snapshotQueueScheduler: ManualFixedScheduler

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
    @WithMockUser(HardhatTestContainer.ACCOUNT_ADDRESS_2)
    fun mustReturnPayoutsForSomeInvestor() {
        val mainAccount = accounts[0]

        val erc20Contract = suppose("simple ERC20 contract is deployed") {
            val future = SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(mainAccount.address),
                listOf(BigInteger("10000")),
                accounts[1].address
            ).sendAsync()
            hardhatContainer.mine()
            future.get()
        }

        suppose("some accounts get ERC20 tokens") {
            erc20Contract.transfer(accounts[1].address, BigInteger("100")).send()
            erc20Contract.transfer(accounts[2].address, BigInteger("200")).send()
            erc20Contract.transfer(accounts[3].address, BigInteger("300")).send()
            erc20Contract.transfer(accounts[4].address, BigInteger("400")).send()
            hardhatContainer.mine()
        }

        val payoutBlock = hardhatContainer.blockNumber()

        erc20Contract.applyWeb3jFilterFix(BlockNumber(BigInteger.ZERO), payoutBlock)

        suppose("some additional transactions of ERC20 token are made") {
            erc20Contract.transfer(accounts[1].address, BigInteger("900")).send()
            erc20Contract.transfer(accounts[5].address, BigInteger("1000")).send()
            erc20Contract.transfer(accounts[6].address, BigInteger("2000")).send()
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

        val createAssetSnapshotResponse = suppose("create asset snapshot request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/asset-snapshots")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        "{\n    \"name\": \"snapshot-name\",\n" +
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
            snapshotQueueScheduler.execute()
        }

        val snapshot = snapshotQueueService.getAssetSnapshotById(createAssetSnapshotResponse.id)!!
        val contractPayout = PayoutStruct(
            BigInteger.ZERO,
            HardhatTestContainer.ACCOUNT_ADDRESS_1,
            "payout-info",
            false,
            erc20Contract.contractAddress,
            snapshot.data?.totalAssetAmount?.rawValue!!,
            snapshot.ignoredHolderAddresses.map { it.rawValue },
            Numeric.hexStringToByteArray(snapshot.data?.merkleRootHash?.value!!),
            BigInteger.valueOf(snapshot.data?.merkleTreeDepth?.toLong()!!),
            snapshot.blockNumber.value,
            snapshot.data?.merkleTreeIpfsHash?.value!!,
            ContractAddress("123456").rawValue,
            BigInteger("60000"),
            BigInteger("60000")
        )

        val payoutManagerContract = suppose("simple payout manager contract is deployed from created snapshot") {
            SimplePayoutManager.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(contractPayout)
            ).send()
        }

        val adminPayouts = suppose("investor payouts are fetched for investor") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/claimable-payouts")
                    .queryParam("chainId", TestData.CHAIN_ID.value.toString())
                    .queryParam("payoutManager", payoutManagerContract.contractAddress)
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, InvestorPayoutsResponse::class.java)
        }

        verify("correct investor payouts are returned") {
            expectThat(adminPayouts)
                .isEqualTo(
                    InvestorPayoutsResponse(
                        listOf(
                            InvestorPayoutResponse(
                                payout = PayoutResponse(
                                    payoutId = contractPayout.payoutId,
                                    payoutOwner = contractPayout.payoutOwner,
                                    payoutInfo = contractPayout.payoutInfo,
                                    isCanceled = contractPayout.isCanceled,

                                    asset = contractPayout.asset,
                                    totalAssetAmount = contractPayout.totalAssetAmount,
                                    ignoredHolderAddresses = contractPayout.ignoredHolderAddresses.toSet(),

                                    assetSnapshotMerkleRoot = snapshot.data!!.merkleRootHash.value,
                                    assetSnapshotMerkleDepth = contractPayout.assetSnapshotMerkleDepth.intValueExact(),
                                    assetSnapshotBlockNumber = contractPayout.assetSnapshotBlockNumber,
                                    assetSnapshotMerkleIpfsHash = contractPayout.assetSnapshotMerkleIpfsHash,

                                    rewardAsset = contractPayout.rewardAsset,
                                    totalRewardAmount = contractPayout.totalRewardAmount,
                                    remainingRewardAmount = contractPayout.remainingRewardAmount
                                ),
                                investor = WalletAddress(accounts[1].address).rawValue,
                                amountClaimed = BigInteger.ZERO,

                                amountClaimable = BigInteger("10000"),
                                balance = BigInteger("100"),
                                proof = adminPayouts.claimablePayouts[0].proof // checked in unit tests
                            )
                        )
                    )
                )
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
