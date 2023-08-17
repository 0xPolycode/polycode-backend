package polycode.controller

import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.web3j.tx.Transfer
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.utils.Convert
import polycode.ControllerTestBase
import polycode.TestData
import polycode.blockchain.SimpleERC20
import polycode.config.CustomHeaders
import polycode.exception.ErrorCode
import polycode.features.api.access.model.result.Project
import polycode.features.asset.balance.model.params.StoreAssetBalanceRequestParams
import polycode.features.asset.balance.model.response.AssetBalanceRequestResponse
import polycode.features.asset.balance.model.response.AssetBalanceRequestsResponse
import polycode.features.asset.balance.model.response.BalanceResponse
import polycode.features.asset.balance.model.result.AssetBalanceRequest
import polycode.features.asset.balance.repository.AssetBalanceRequestRepository
import polycode.generated.jooq.enums.UserIdentifierType
import polycode.generated.jooq.id.ApiKeyId
import polycode.generated.jooq.id.AssetBalanceRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import polycode.generated.jooq.tables.AssetBalanceRequestTable
import polycode.generated.jooq.tables.records.ApiKeyRecord
import polycode.generated.jooq.tables.records.ProjectRecord
import polycode.generated.jooq.tables.records.UserIdentifierRecord
import polycode.model.ScreenConfig
import polycode.testcontainers.HardhatTestContainer
import polycode.util.AssetType
import polycode.util.Balance
import polycode.util.BaseUrl
import polycode.util.BlockNumber
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.SignedMessage
import polycode.util.Status
import polycode.util.WalletAddress
import java.math.BigInteger
import java.util.UUID

class AssetBalanceRequestControllerApiTest : ControllerTestBase() {

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
    private lateinit var assetBalanceRequestRepository: AssetBalanceRequestRepository

    @Autowired
    private lateinit var dslContext: DSLContext

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
    }

    @Test
    fun mustCorrectlyCreateAssetBalanceRequestForSomeToken() {
        val tokenAddress = ContractAddress("cafebabe")
        val blockNumber = BlockNumber(BigInteger.TEN)
        val walletAddress = WalletAddress("a")

        val response = suppose("request to create asset balance request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/balance")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "asset_type" : "TOKEN",
                                "block_number": "${blockNumber.value}",
                                "wallet_address": "${walletAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, AssetBalanceRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    AssetBalanceRequestResponse(
                        id = response.id,
                        projectId = PROJECT_ID,
                        status = Status.PENDING,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-balance/${response.id.value}/action",
                        tokenAddress = tokenAddress.rawValue,
                        assetType = AssetType.TOKEN,
                        blockNumber = blockNumber.value,
                        walletAddress = walletAddress.rawValue,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        balance = null,
                        messageToSign = "Verification message ID to sign: ${response.id.value}",
                        signedMessage = null,
                        createdAt = response.createdAt
                    )
                )

            expectThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("asset balance request is correctly stored in database") {
            val storedRequest = assetBalanceRequestRepository.getById(response.id)

            expectThat(storedRequest)
                .isEqualTo(
                    AssetBalanceRequest(
                        id = response.id,
                        projectId = PROJECT_ID,
                        chainId = PROJECT.chainId,
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-balance/${response.id.value}/action",
                        tokenAddress = tokenAddress,
                        blockNumber = blockNumber,
                        requestedWalletAddress = walletAddress,
                        actualWalletAddress = null,
                        signedMessage = null,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        createdAt = storedRequest!!.createdAt
                    )
                )

            expectThat(storedRequest.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyCreateAssetBalanceRequestForSomeTokenWithRedirectUrl() {
        val tokenAddress = ContractAddress("cafebabe")
        val blockNumber = BlockNumber(BigInteger.TEN)
        val walletAddress = WalletAddress("a")
        val redirectUrl = "https://custom-url/\${id}"

        val response = suppose("request to create asset balance request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/balance")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "redirect_url": "$redirectUrl",
                                "token_address" : "${tokenAddress.rawValue}",
                                "asset_type" : "TOKEN",
                                "block_number": "${blockNumber.value}",
                                "wallet_address": "${walletAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, AssetBalanceRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    AssetBalanceRequestResponse(
                        id = response.id,
                        projectId = PROJECT_ID,
                        status = Status.PENDING,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = "https://custom-url/${response.id.value}",
                        tokenAddress = tokenAddress.rawValue,
                        assetType = AssetType.TOKEN,
                        blockNumber = blockNumber.value,
                        walletAddress = walletAddress.rawValue,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        balance = null,
                        messageToSign = "Verification message ID to sign: ${response.id.value}",
                        signedMessage = null,
                        createdAt = response.createdAt
                    )
                )

            expectThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("asset balance request is correctly stored in database") {
            val storedRequest = assetBalanceRequestRepository.getById(response.id)

            expectThat(storedRequest)
                .isEqualTo(
                    AssetBalanceRequest(
                        id = response.id,
                        projectId = PROJECT_ID,
                        chainId = PROJECT.chainId,
                        redirectUrl = "https://custom-url/${response.id.value}",
                        tokenAddress = tokenAddress,
                        blockNumber = blockNumber,
                        requestedWalletAddress = walletAddress,
                        actualWalletAddress = null,
                        signedMessage = null,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        createdAt = storedRequest!!.createdAt
                    )
                )

            expectThat(storedRequest.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyCreateAssetBalanceRequestForNativeAsset() {
        val blockNumber = BlockNumber(BigInteger.TEN)
        val walletAddress = WalletAddress("a")

        val response = suppose("request to create asset balance request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/balance")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "asset_type" : "NATIVE",
                                "block_number": "${blockNumber.value}",
                                "wallet_address": "${walletAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, AssetBalanceRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    AssetBalanceRequestResponse(
                        id = response.id,
                        projectId = PROJECT_ID,
                        status = Status.PENDING,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-balance/${response.id.value}/action",
                        tokenAddress = null,
                        assetType = AssetType.NATIVE,
                        blockNumber = blockNumber.value,
                        walletAddress = walletAddress.rawValue,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        balance = null,
                        messageToSign = "Verification message ID to sign: ${response.id.value}",
                        signedMessage = null,
                        createdAt = response.createdAt
                    )
                )

            expectThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("asset balance request is correctly stored in database") {
            val storedRequest = assetBalanceRequestRepository.getById(response.id)

            expectThat(storedRequest)
                .isEqualTo(
                    AssetBalanceRequest(
                        id = response.id,
                        projectId = PROJECT_ID,
                        chainId = PROJECT.chainId,
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-balance/${response.id.value}/action",
                        tokenAddress = null,
                        blockNumber = blockNumber,
                        requestedWalletAddress = walletAddress,
                        actualWalletAddress = null,
                        signedMessage = null,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        createdAt = storedRequest!!.createdAt
                    )
                )

            expectThat(storedRequest.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyCreateAssetBalanceRequestForNativeAssetWithRedirectUrl() {
        val blockNumber = BlockNumber(BigInteger.TEN)
        val walletAddress = WalletAddress("a")
        val redirectUrl = "https://custom-url/\${id}"

        val response = suppose("request to create asset balance request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/balance")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "redirect_url": "$redirectUrl",
                                "asset_type" : "NATIVE",
                                "block_number": "${blockNumber.value}",
                                "wallet_address": "${walletAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, AssetBalanceRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    AssetBalanceRequestResponse(
                        id = response.id,
                        projectId = PROJECT_ID,
                        status = Status.PENDING,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = "https://custom-url/${response.id.value}",
                        tokenAddress = null,
                        assetType = AssetType.NATIVE,
                        blockNumber = blockNumber.value,
                        walletAddress = walletAddress.rawValue,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        balance = null,
                        messageToSign = "Verification message ID to sign: ${response.id.value}",
                        signedMessage = null,
                        createdAt = response.createdAt
                    )
                )

            expectThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("asset balance request is correctly stored in database") {
            val storedRequest = assetBalanceRequestRepository.getById(response.id)

            expectThat(storedRequest)
                .isEqualTo(
                    AssetBalanceRequest(
                        id = response.id,
                        projectId = PROJECT_ID,
                        chainId = PROJECT.chainId,
                        redirectUrl = "https://custom-url/${response.id.value}",
                        tokenAddress = null,
                        blockNumber = blockNumber,
                        requestedWalletAddress = walletAddress,
                        actualWalletAddress = null,
                        signedMessage = null,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        createdAt = storedRequest!!.createdAt
                    )
                )

            expectThat(storedRequest.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenTokenAddressIsMissingForTokenAssetType() {
        val blockNumber = BlockNumber(BigInteger.TEN)
        val walletAddress = WalletAddress("a")

        verify("400 is returned for missing token address") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/balance")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "asset_type" : "TOKEN",
                                "block_number": "${blockNumber.value}",
                                "wallet_address": "${walletAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.MISSING_TOKEN_ADDRESS)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenTokenAddressIsSpecifiedForNativeAssetType() {
        val tokenAddress = ContractAddress("cafebabe")
        val blockNumber = BlockNumber(BigInteger.TEN)
        val walletAddress = WalletAddress("a")

        verify("400 is returned for non-allowed token address") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/balance")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "asset_type": "NATIVE",
                                "block_number": "${blockNumber.value}",
                                "wallet_address": "${walletAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.TOKEN_ADDRESS_NOT_ALLOWED)
        }
    }

    @Test
    fun mustReturn401UnauthorizedWhenCreatingAssetBalanceRequestWithInvalidApiKey() {
        val tokenAddress = ContractAddress("cafebabe")
        val blockNumber = BlockNumber(BigInteger.TEN)
        val walletAddress = WalletAddress("a")

        verify("401 is returned for invalid API key") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/balance")
                    .header(CustomHeaders.API_KEY_HEADER, "invalid-api-key")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "block_number": "${blockNumber.value}",
                                "wallet_address": "${walletAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isUnauthorized)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.NON_EXISTENT_API_KEY)
        }
    }

    @Test
    fun mustCorrectlyFetchAssetBalanceRequestForSomeToken() {
        val mainAccount = accounts[0]
        val walletAddress = WalletAddress("0x865f603F42ca1231e5B5F90e15663b0FE19F0b21")
        val assetBalance = Balance(BigInteger("10000"))

        val contract = suppose("simple asset contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(walletAddress.rawValue),
                listOf(assetBalance.rawValue),
                mainAccount.address
            ).send()
        }

        val tokenAddress = ContractAddress(contract.contractAddress)
        val blockNumber = hardhatContainer.blockNumber()

        val createResponse = suppose("request to create asset balance request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/balance")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "asset_type" : "TOKEN",
                                "block_number": "${blockNumber.value}",
                                "wallet_address": "${walletAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(createResponse.response.contentAsString, AssetBalanceRequestResponse::class.java)
        }

        val id = AssetBalanceRequestId(UUID.fromString("7d86b0ac-a9a6-40fc-ac6d-2a29ca687f73"))

        suppose("ID from pre-signed message is used in database") {
            dslContext.update(AssetBalanceRequestTable)
                .set(AssetBalanceRequestTable.ID, id)
                .set(AssetBalanceRequestTable.REDIRECT_URL, "https://example.com/${id.value}")
                .where(AssetBalanceRequestTable.ID.eq(createResponse.id))
                .execute()
        }

        val signedMessage = SignedMessage(
            "0xfc90c8aa9f2164234b8826144d8ecfc287b5d7c168d0e9d284baf76dbef55c4c5761cf46e34b7cdb72cc97f1fb1c19f315ee7a" +
                "430dd6111fa6c693b41c96c5501c"
        )

        suppose("signed message is attached to asset balance request") {
            assetBalanceRequestRepository.setSignedMessage(id, walletAddress, signedMessage)
        }

        val fetchResponse = suppose("request to fetch asset balance request is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/balance/${id.value}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, AssetBalanceRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    AssetBalanceRequestResponse(
                        id = id,
                        projectId = PROJECT_ID,
                        status = Status.SUCCESS,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = "https://example.com/${id.value}",
                        tokenAddress = tokenAddress.rawValue,
                        assetType = AssetType.TOKEN,
                        blockNumber = blockNumber.value,
                        walletAddress = walletAddress.rawValue,
                        arbitraryData = createResponse.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        balance = BalanceResponse(
                            wallet = walletAddress.rawValue,
                            blockNumber = blockNumber.value,
                            timestamp = fetchResponse.balance!!.timestamp,
                            amount = assetBalance.rawValue
                        ),
                        messageToSign = "Verification message ID to sign: ${id.value}",
                        signedMessage = signedMessage.value,
                        createdAt = fetchResponse.createdAt
                    )
                )

            expectThat(fetchResponse.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchAssetBalanceRequestForSomeTokenWhenCustomRpcUrlIsSpecified() {
        val mainAccount = accounts[0]
        val walletAddress = WalletAddress("0x865f603F42ca1231e5B5F90e15663b0FE19F0b21")
        val assetBalance = Balance(BigInteger("10000"))

        val contract = suppose("simple asset contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(walletAddress.rawValue),
                listOf(assetBalance.rawValue),
                mainAccount.address
            ).send()
        }

        val tokenAddress = ContractAddress(contract.contractAddress)
        val blockNumber = hardhatContainer.blockNumber()

        val (projectId, chainId, apiKey) = suppose("project with customRpcUrl is inserted into database") {
            insertProjectWithCustomRpcUrl()
        }

        val createResponse = suppose("request to create asset balance request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/balance")
                    .header(CustomHeaders.API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "asset_type" : "TOKEN",
                                "block_number": "${blockNumber.value}",
                                "wallet_address": "${walletAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(createResponse.response.contentAsString, AssetBalanceRequestResponse::class.java)
        }

        val id = AssetBalanceRequestId(UUID.fromString("7d86b0ac-a9a6-40fc-ac6d-2a29ca687f73"))

        suppose("ID from pre-signed message is used in database") {
            dslContext.update(AssetBalanceRequestTable)
                .set(AssetBalanceRequestTable.ID, id)
                .set(AssetBalanceRequestTable.REDIRECT_URL, "https://example.com/${id.value}")
                .where(AssetBalanceRequestTable.ID.eq(createResponse.id))
                .execute()
        }

        val signedMessage = SignedMessage(
            "0xfc90c8aa9f2164234b8826144d8ecfc287b5d7c168d0e9d284baf76dbef55c4c5761cf46e34b7cdb72cc97f1fb1c19f315ee7a" +
                "430dd6111fa6c693b41c96c5501c"
        )

        suppose("signed message is attached to asset balance request") {
            assetBalanceRequestRepository.setSignedMessage(id, walletAddress, signedMessage)
        }

        val fetchResponse = suppose("request to fetch asset balance request is made") {
            val fetchResponse = mockMvc.perform(MockMvcRequestBuilders.get("/v1/balance/${id.value}"))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, AssetBalanceRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    AssetBalanceRequestResponse(
                        id = id,
                        projectId = projectId,
                        status = Status.SUCCESS,
                        chainId = chainId.value,
                        redirectUrl = "https://example.com/${id.value}",
                        tokenAddress = tokenAddress.rawValue,
                        assetType = AssetType.TOKEN,
                        blockNumber = blockNumber.value,
                        walletAddress = walletAddress.rawValue,
                        arbitraryData = createResponse.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        balance = BalanceResponse(
                            wallet = walletAddress.rawValue,
                            blockNumber = blockNumber.value,
                            timestamp = fetchResponse.balance!!.timestamp,
                            amount = assetBalance.rawValue
                        ),
                        messageToSign = "Verification message ID to sign: ${id.value}",
                        signedMessage = signedMessage.value,
                        createdAt = fetchResponse.createdAt
                    )
                )

            expectThat(fetchResponse.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchAssetBalanceRequestForNativeAsset() {
        val mainAccount = accounts[0]
        val walletAddress = WalletAddress("0x865f603F42ca1231e5B5F90e15663b0FE19F0b21")
        val amount = Balance(BigInteger("10000"))

        suppose("some regular transfer transaction is made") {
            Transfer.sendFunds(
                hardhatContainer.web3j,
                mainAccount,
                walletAddress.rawValue,
                amount.rawValue.toBigDecimal(),
                Convert.Unit.WEI
            ).send()
        }

        val blockNumber = hardhatContainer.blockNumber()

        val createResponse = suppose("request to create asset balance request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/balance")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "asset_type" : "NATIVE",
                                "block_number": "${blockNumber.value}",
                                "wallet_address": "${walletAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(createResponse.response.contentAsString, AssetBalanceRequestResponse::class.java)
        }

        val id = AssetBalanceRequestId(UUID.fromString("7d86b0ac-a9a6-40fc-ac6d-2a29ca687f73"))

        suppose("ID from pre-signed message is used in database") {
            dslContext.update(AssetBalanceRequestTable)
                .set(AssetBalanceRequestTable.ID, id)
                .set(AssetBalanceRequestTable.REDIRECT_URL, "https://example.com/${id.value}")
                .where(AssetBalanceRequestTable.ID.eq(createResponse.id))
                .execute()
        }

        val signedMessage = SignedMessage(
            "0xfc90c8aa9f2164234b8826144d8ecfc287b5d7c168d0e9d284baf76dbef55c4c5761cf46e34b7cdb72cc97f1fb1c19f315ee7a" +
                "430dd6111fa6c693b41c96c5501c"
        )

        suppose("signed message is attached to asset balance request") {
            assetBalanceRequestRepository.setSignedMessage(id, walletAddress, signedMessage)
        }

        val fetchResponse = suppose("request to fetch asset balance request is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/balance/${id.value}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, AssetBalanceRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    AssetBalanceRequestResponse(
                        id = id,
                        projectId = PROJECT_ID,
                        status = Status.SUCCESS,
                        chainId = PROJECT.chainId.value,
                        redirectUrl = "https://example.com/${id.value}",
                        tokenAddress = null,
                        assetType = AssetType.NATIVE,
                        blockNumber = blockNumber.value,
                        walletAddress = walletAddress.rawValue,
                        arbitraryData = createResponse.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        balance = BalanceResponse(
                            wallet = walletAddress.rawValue,
                            blockNumber = blockNumber.value,
                            timestamp = fetchResponse.balance!!.timestamp,
                            amount = amount.rawValue
                        ),
                        messageToSign = "Verification message ID to sign: ${id.value}",
                        signedMessage = signedMessage.value,
                        createdAt = fetchResponse.createdAt
                    )
                )

            expectThat(fetchResponse.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchAssetBalanceRequestForNativeAssetWhenCustomRpcUrlIsSpecified() {
        val mainAccount = accounts[0]
        val walletAddress = WalletAddress("0x865f603F42ca1231e5B5F90e15663b0FE19F0b21")
        val amount = Balance(BigInteger("10000"))

        suppose("some regular transfer transaction is made") {
            Transfer.sendFunds(
                hardhatContainer.web3j,
                mainAccount,
                walletAddress.rawValue,
                amount.rawValue.toBigDecimal(),
                Convert.Unit.WEI
            ).send()
        }

        val blockNumber = hardhatContainer.blockNumber()

        val (projectId, chainId, apiKey) = suppose("project with customRpcUrl is inserted into database") {
            insertProjectWithCustomRpcUrl()
        }

        val createResponse = suppose("request to create asset balance request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/balance")
                    .header(CustomHeaders.API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "asset_type" : "NATIVE",
                                "block_number": "${blockNumber.value}",
                                "wallet_address": "${walletAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(createResponse.response.contentAsString, AssetBalanceRequestResponse::class.java)
        }

        val id = AssetBalanceRequestId(UUID.fromString("7d86b0ac-a9a6-40fc-ac6d-2a29ca687f73"))

        suppose("ID from pre-signed message is used in database") {
            dslContext.update(AssetBalanceRequestTable)
                .set(AssetBalanceRequestTable.ID, id)
                .set(AssetBalanceRequestTable.REDIRECT_URL, "https://example.com/${id.value}")
                .where(AssetBalanceRequestTable.ID.eq(createResponse.id))
                .execute()
        }

        val signedMessage = SignedMessage(
            "0xfc90c8aa9f2164234b8826144d8ecfc287b5d7c168d0e9d284baf76dbef55c4c5761cf46e34b7cdb72cc97f1fb1c19f315ee7a" +
                "430dd6111fa6c693b41c96c5501c"
        )

        suppose("signed message is attached to asset balance request") {
            assetBalanceRequestRepository.setSignedMessage(id, walletAddress, signedMessage)
        }

        val fetchResponse = suppose("request to fetch asset balance request is made") {
            val fetchResponse = mockMvc.perform(MockMvcRequestBuilders.get("/v1/balance/${id.value}"))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, AssetBalanceRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    AssetBalanceRequestResponse(
                        id = id,
                        projectId = projectId,
                        status = Status.SUCCESS,
                        chainId = chainId.value,
                        redirectUrl = "https://example.com/${id.value}",
                        tokenAddress = null,
                        assetType = AssetType.NATIVE,
                        blockNumber = blockNumber.value,
                        walletAddress = walletAddress.rawValue,
                        arbitraryData = createResponse.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        balance = BalanceResponse(
                            wallet = walletAddress.rawValue,
                            blockNumber = blockNumber.value,
                            timestamp = fetchResponse.balance!!.timestamp,
                            amount = amount.rawValue
                        ),
                        messageToSign = "Verification message ID to sign: ${id.value}",
                        signedMessage = signedMessage.value,
                        createdAt = fetchResponse.createdAt
                    )
                )

            expectThat(fetchResponse.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchAssetBalanceRequestsByProjectId() {
        val mainAccount = accounts[0]
        val walletAddress = WalletAddress("0x865f603F42ca1231e5B5F90e15663b0FE19F0b21")
        val assetBalance = Balance(BigInteger("10000"))

        val contract = suppose("simple asset contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(walletAddress.rawValue),
                listOf(assetBalance.rawValue),
                mainAccount.address
            ).send()
        }

        val tokenAddress = ContractAddress(contract.contractAddress)
        val blockNumber = hardhatContainer.blockNumber()

        val createResponse = suppose("request to create asset balance request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/balance")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "asset_type" : "TOKEN",
                                "block_number": "${blockNumber.value}",
                                "wallet_address": "${walletAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(createResponse.response.contentAsString, AssetBalanceRequestResponse::class.java)
        }

        val id = AssetBalanceRequestId(UUID.fromString("7d86b0ac-a9a6-40fc-ac6d-2a29ca687f73"))

        suppose("ID from pre-signed message is used in database") {
            dslContext.update(AssetBalanceRequestTable)
                .set(AssetBalanceRequestTable.ID, id)
                .set(AssetBalanceRequestTable.REDIRECT_URL, "https://example.com/${id.value}")
                .where(AssetBalanceRequestTable.ID.eq(createResponse.id))
                .execute()
        }

        val signedMessage = SignedMessage(
            "0xfc90c8aa9f2164234b8826144d8ecfc287b5d7c168d0e9d284baf76dbef55c4c5761cf46e34b7cdb72cc97f1fb1c19f315ee7a" +
                "430dd6111fa6c693b41c96c5501c"
        )

        suppose("signed message is attached to asset balance request") {
            assetBalanceRequestRepository.setSignedMessage(id, walletAddress, signedMessage)
        }

        val fetchResponse = suppose("request to fetch asset balance requests by project ID is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/balance/by-project/${PROJECT_ID.value}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, AssetBalanceRequestsResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    AssetBalanceRequestsResponse(
                        listOf(
                            AssetBalanceRequestResponse(
                                id = id,
                                projectId = PROJECT_ID,
                                status = Status.SUCCESS,
                                chainId = PROJECT.chainId.value,
                                redirectUrl = "https://example.com/${id.value}",
                                tokenAddress = tokenAddress.rawValue,
                                assetType = AssetType.TOKEN,
                                blockNumber = blockNumber.value,
                                walletAddress = walletAddress.rawValue,
                                arbitraryData = createResponse.arbitraryData,
                                screenConfig = ScreenConfig(
                                    beforeActionMessage = "before-action-message",
                                    afterActionMessage = "after-action-message"
                                ),
                                balance = BalanceResponse(
                                    wallet = walletAddress.rawValue,
                                    blockNumber = blockNumber.value,
                                    timestamp = fetchResponse.requests[0].balance!!.timestamp,
                                    amount = assetBalance.rawValue
                                ),
                                messageToSign = "Verification message ID to sign: ${id.value}",
                                signedMessage = signedMessage.value,
                                createdAt = fetchResponse.requests[0].createdAt
                            )
                        )
                    )
                )

            expectThat(fetchResponse.requests[0].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchAssetBalanceRequestsByProjectIdWhenCustomRpcUrlIsSpecified() {
        val mainAccount = accounts[0]
        val walletAddress = WalletAddress("0x865f603F42ca1231e5B5F90e15663b0FE19F0b21")
        val assetBalance = Balance(BigInteger("10000"))

        val contract = suppose("simple asset contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(walletAddress.rawValue),
                listOf(assetBalance.rawValue),
                mainAccount.address
            ).send()
        }

        val tokenAddress = ContractAddress(contract.contractAddress)
        val blockNumber = hardhatContainer.blockNumber()

        val (projectId, chainId, apiKey) = suppose("project with customRpcUrl is inserted into database") {
            insertProjectWithCustomRpcUrl()
        }

        val createResponse = suppose("request to create asset balance request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/balance")
                    .header(CustomHeaders.API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "asset_type" : "TOKEN",
                                "block_number": "${blockNumber.value}",
                                "wallet_address": "${walletAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "before_action_message": "before-action-message",
                                    "after_action_message": "after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(createResponse.response.contentAsString, AssetBalanceRequestResponse::class.java)
        }

        val id = AssetBalanceRequestId(UUID.fromString("7d86b0ac-a9a6-40fc-ac6d-2a29ca687f73"))

        suppose("ID from pre-signed message is used in database") {
            dslContext.update(AssetBalanceRequestTable)
                .set(AssetBalanceRequestTable.ID, id)
                .set(AssetBalanceRequestTable.REDIRECT_URL, "https://example.com/${id.value}")
                .where(AssetBalanceRequestTable.ID.eq(createResponse.id))
                .execute()
        }

        val signedMessage = SignedMessage(
            "0xfc90c8aa9f2164234b8826144d8ecfc287b5d7c168d0e9d284baf76dbef55c4c5761cf46e34b7cdb72cc97f1fb1c19f315ee7a" +
                "430dd6111fa6c693b41c96c5501c"
        )

        suppose("signed message is attached to asset balance request") {
            assetBalanceRequestRepository.setSignedMessage(id, walletAddress, signedMessage)
        }

        val fetchResponse = suppose("request to fetch asset balance requests by project ID is made") {
            val fetchResponse = mockMvc.perform(MockMvcRequestBuilders.get("/v1/balance/by-project/${projectId.value}"))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, AssetBalanceRequestsResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    AssetBalanceRequestsResponse(
                        listOf(
                            AssetBalanceRequestResponse(
                                id = id,
                                projectId = projectId,
                                status = Status.SUCCESS,
                                chainId = chainId.value,
                                redirectUrl = "https://example.com/${id.value}",
                                tokenAddress = tokenAddress.rawValue,
                                assetType = AssetType.TOKEN,
                                blockNumber = blockNumber.value,
                                walletAddress = walletAddress.rawValue,
                                arbitraryData = createResponse.arbitraryData,
                                screenConfig = ScreenConfig(
                                    beforeActionMessage = "before-action-message",
                                    afterActionMessage = "after-action-message"
                                ),
                                balance = BalanceResponse(
                                    wallet = walletAddress.rawValue,
                                    blockNumber = blockNumber.value,
                                    timestamp = fetchResponse.requests[0].balance!!.timestamp,
                                    amount = assetBalance.rawValue
                                ),
                                messageToSign = "Verification message ID to sign: ${id.value}",
                                signedMessage = signedMessage.value,
                                createdAt = fetchResponse.requests[0].createdAt
                            )
                        )
                    )
                )

            expectThat(fetchResponse.requests[0].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustReturn404NotFoundForNonExistentAssetBalanceRequest() {
        verify("404 is returned for non-existent asset balance request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/balance/${UUID.randomUUID()}")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustCorrectlyAttachSignedMessage() {
        val id = AssetBalanceRequestId(UUID.randomUUID())

        suppose("some asset balance request without signed message exists in database") {
            assetBalanceRequestRepository.store(
                StoreAssetBalanceRequestParams(
                    id = id,
                    projectId = PROJECT_ID,
                    chainId = TestData.CHAIN_ID,
                    redirectUrl = "https://example.com/${id.value}",
                    tokenAddress = ContractAddress("a"),
                    blockNumber = BlockNumber(BigInteger.TEN),
                    requestedWalletAddress = WalletAddress("b"),
                    arbitraryData = TestData.EMPTY_JSON_OBJECT,
                    screenConfig = ScreenConfig(
                        beforeActionMessage = "before-action-message",
                        afterActionMessage = "after-action-message"
                    ),
                    createdAt = TestData.TIMESTAMP
                )
            )
        }

        val walletAddress = WalletAddress("c")
        val signedMessage = SignedMessage("signed-message")

        suppose("request to attach signed message to asset balance request is made") {
            mockMvc.perform(
                MockMvcRequestBuilders.put("/v1/balance/${id.value}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "wallet_address": "${walletAddress.rawValue}",
                                "signed_message": "${signedMessage.value}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        }

        verify("signed message is correctly attached to asset balance request") {
            val storedRequest = assetBalanceRequestRepository.getById(id)

            expectThat(storedRequest?.actualWalletAddress)
                .isEqualTo(walletAddress)
            expectThat(storedRequest?.signedMessage)
                .isEqualTo(signedMessage)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenSignedMessageIsNotAttached() {
        val id = AssetBalanceRequestId(UUID.randomUUID())
        val walletAddress = WalletAddress("c")
        val signedMessage = SignedMessage("signed-message")

        suppose("some asset balance request with signed message exists in database") {
            assetBalanceRequestRepository.store(
                StoreAssetBalanceRequestParams(
                    id = id,
                    projectId = PROJECT_ID,
                    chainId = TestData.CHAIN_ID,
                    redirectUrl = "https://example.com/${id.value}",
                    tokenAddress = ContractAddress("a"),
                    blockNumber = BlockNumber(BigInteger.TEN),
                    requestedWalletAddress = WalletAddress("b"),
                    arbitraryData = TestData.EMPTY_JSON_OBJECT,
                    screenConfig = ScreenConfig(
                        beforeActionMessage = "before-action-message",
                        afterActionMessage = "after-action-message"
                    ),
                    createdAt = TestData.TIMESTAMP
                )
            )
            assetBalanceRequestRepository.setSignedMessage(id, walletAddress, signedMessage)
        }

        verify("400 is returned when attaching signed message") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.put("/v1/balance/${id.value}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "wallet_address": "${WalletAddress("dead").rawValue}",
                                "signed_message": "different-signed-message"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.SIGNED_MESSAGE_ALREADY_SET)
        }

        verify("signed message is not changed in database") {
            val storedRequest = assetBalanceRequestRepository.getById(id)

            expectThat(storedRequest?.actualWalletAddress)
                .isEqualTo(walletAddress)
            expectThat(storedRequest?.signedMessage)
                .isEqualTo(signedMessage)
        }
    }

    private fun insertProjectWithCustomRpcUrl(): Triple<ProjectId, ChainId, String> {
        val projectId = ProjectId(UUID.randomUUID())
        val chainId = ChainId(1337L)

        dslContext.executeInsert(
            ProjectRecord(
                id = projectId,
                ownerId = PROJECT.ownerId,
                baseRedirectUrl = PROJECT.baseRedirectUrl,
                chainId = chainId,
                customRpcUrl = "http://localhost:${hardhatContainer.mappedPort}",
                createdAt = PROJECT.createdAt
            )
        )

        val apiKey = "another-api-key"

        dslContext.executeInsert(
            ApiKeyRecord(
                id = ApiKeyId(UUID.randomUUID()),
                projectId = projectId,
                apiKey = apiKey,
                createdAt = TestData.TIMESTAMP
            )
        )

        return Triple(projectId, chainId, apiKey)
    }
}
