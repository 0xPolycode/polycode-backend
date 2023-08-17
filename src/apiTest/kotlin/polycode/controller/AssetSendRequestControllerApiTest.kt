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
import polycode.features.asset.send.model.params.StoreAssetSendRequestParams
import polycode.features.asset.send.model.response.AssetSendRequestResponse
import polycode.features.asset.send.model.response.AssetSendRequestsResponse
import polycode.features.asset.send.model.result.AssetSendRequest
import polycode.features.asset.send.repository.AssetSendRequestRepository
import polycode.generated.jooq.enums.UserIdentifierType
import polycode.generated.jooq.id.ApiKeyId
import polycode.generated.jooq.id.AssetSendRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import polycode.generated.jooq.tables.records.ApiKeyRecord
import polycode.generated.jooq.tables.records.ProjectRecord
import polycode.generated.jooq.tables.records.UserIdentifierRecord
import polycode.model.ScreenConfig
import polycode.model.response.EventArgumentResponse
import polycode.model.response.EventArgumentResponseType
import polycode.model.response.EventInfoResponse
import polycode.model.response.TransactionResponse
import polycode.testcontainers.HardhatTestContainer
import polycode.util.AssetType
import polycode.util.Balance
import polycode.util.BaseUrl
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.Status
import polycode.util.TransactionHash
import polycode.util.WalletAddress
import java.math.BigInteger
import java.util.UUID

class AssetSendRequestControllerApiTest : ControllerTestBase() {

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
        private val EVENTS = listOf(
            EventInfoResponse(
                signature = "Transfer(address,address,uint256)",
                arguments = listOf(
                    EventArgumentResponse(
                        name = "from",
                        type = EventArgumentResponseType.VALUE,
                        value = HardhatTestContainer.ACCOUNT_ADDRESS_1,
                        hash = null
                    ),
                    EventArgumentResponse(
                        name = "to",
                        type = EventArgumentResponseType.VALUE,
                        value = HardhatTestContainer.ACCOUNT_ADDRESS_2,
                        hash = null
                    ),
                    EventArgumentResponse(
                        name = "value",
                        type = EventArgumentResponseType.VALUE,
                        value = "10",
                        hash = null
                    )
                )
            )
        )
    }

    private val accounts = HardhatTestContainer.ACCOUNTS

    @Autowired
    private lateinit var assetSendRequestRepository: AssetSendRequestRepository

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
    fun mustCorrectlyCreateAssetSendRequestForSomeToken() {
        val tokenAddress = ContractAddress("cafebabe")
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress("b")
        val recipientAddress = WalletAddress("c")

        val response = suppose("request to create asset send request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "asset_type": "TOKEN",
                                "amount": "${amount.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
                                "recipient_address": "${recipientAddress.rawValue}",
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

            objectMapper.readValue(response.response.contentAsString, AssetSendRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    AssetSendRequestResponse(
                        id = response.id,
                        projectId = PROJECT_ID,
                        status = Status.PENDING,
                        chainId = PROJECT.chainId.value,
                        tokenAddress = tokenAddress.rawValue,
                        assetType = AssetType.TOKEN,
                        amount = amount.rawValue,
                        senderAddress = senderAddress.rawValue,
                        recipientAddress = recipientAddress.rawValue,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-send/${response.id.value}/action",
                        sendTx = TransactionResponse(
                            txHash = null,
                            from = senderAddress.rawValue,
                            to = tokenAddress.rawValue,
                            data = response.sendTx.data,
                            value = BigInteger.ZERO,
                            blockConfirmations = null,
                            timestamp = null
                        ),
                        createdAt = response.createdAt,
                        events = null
                    )
                )

            expectThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("asset send request is correctly stored in database") {
            val storedRequest = assetSendRequestRepository.getById(response.id)

            expectThat(storedRequest)
                .isEqualTo(
                    AssetSendRequest(
                        id = response.id,
                        projectId = PROJECT_ID,
                        chainId = PROJECT.chainId,
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-send/${response.id.value}/action",
                        tokenAddress = tokenAddress,
                        assetAmount = amount,
                        assetSenderAddress = senderAddress,
                        assetRecipientAddress = recipientAddress,
                        txHash = null,
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
    fun mustCorrectlyCreateAssetSendRequestForSomeTokenWithRedirectUrl() {
        val tokenAddress = ContractAddress("cafebabe")
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress("b")
        val recipientAddress = WalletAddress("c")
        val redirectUrl = "https://custom-url/\${id}"

        val response = suppose("request to create asset send request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "redirect_url": "$redirectUrl",
                                "token_address": "${tokenAddress.rawValue}",
                                "asset_type": "TOKEN",
                                "amount": "${amount.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
                                "recipient_address": "${recipientAddress.rawValue}",
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

            objectMapper.readValue(response.response.contentAsString, AssetSendRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    AssetSendRequestResponse(
                        id = response.id,
                        projectId = PROJECT_ID,
                        status = Status.PENDING,
                        chainId = PROJECT.chainId.value,
                        tokenAddress = tokenAddress.rawValue,
                        assetType = AssetType.TOKEN,
                        amount = amount.rawValue,
                        senderAddress = senderAddress.rawValue,
                        recipientAddress = recipientAddress.rawValue,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        redirectUrl = "https://custom-url/${response.id.value}",
                        sendTx = TransactionResponse(
                            txHash = null,
                            from = senderAddress.rawValue,
                            to = tokenAddress.rawValue,
                            data = response.sendTx.data,
                            value = BigInteger.ZERO,
                            blockConfirmations = null,
                            timestamp = null
                        ),
                        createdAt = response.createdAt,
                        events = null
                    )
                )

            expectThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("asset send request is correctly stored in database") {
            val storedRequest = assetSendRequestRepository.getById(response.id)

            expectThat(storedRequest)
                .isEqualTo(
                    AssetSendRequest(
                        id = response.id,
                        projectId = PROJECT_ID,
                        chainId = PROJECT.chainId,
                        redirectUrl = "https://custom-url/${response.id.value}",
                        tokenAddress = tokenAddress,
                        assetAmount = amount,
                        assetSenderAddress = senderAddress,
                        assetRecipientAddress = recipientAddress,
                        txHash = null,
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
    fun mustCorrectlyCreateAssetSendRequestForNativeAsset() {
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress("b")
        val recipientAddress = WalletAddress("c")

        val response = suppose("request to create asset send request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "asset_type": "NATIVE",
                                "amount": "${amount.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
                                "recipient_address": "${recipientAddress.rawValue}",
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

            objectMapper.readValue(response.response.contentAsString, AssetSendRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    AssetSendRequestResponse(
                        id = response.id,
                        projectId = PROJECT_ID,
                        status = Status.PENDING,
                        chainId = PROJECT.chainId.value,
                        tokenAddress = null,
                        assetType = AssetType.NATIVE,
                        amount = amount.rawValue,
                        senderAddress = senderAddress.rawValue,
                        recipientAddress = recipientAddress.rawValue,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-send/${response.id.value}/action",
                        sendTx = TransactionResponse(
                            txHash = null,
                            from = senderAddress.rawValue,
                            to = recipientAddress.rawValue,
                            data = null,
                            value = amount.rawValue,
                            blockConfirmations = null,
                            timestamp = null
                        ),
                        createdAt = response.createdAt,
                        events = null
                    )
                )

            expectThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("asset send request is correctly stored in database") {
            val storedRequest = assetSendRequestRepository.getById(response.id)

            expectThat(storedRequest)
                .isEqualTo(
                    AssetSendRequest(
                        id = response.id,
                        projectId = PROJECT_ID,
                        chainId = PROJECT.chainId,
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-send/${response.id.value}/action",
                        tokenAddress = null,
                        assetAmount = amount,
                        assetSenderAddress = senderAddress,
                        assetRecipientAddress = recipientAddress,
                        txHash = null,
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
    fun mustCorrectlyCreateAssetSendRequestForNativeAssetWithRedirectUrl() {
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress("b")
        val recipientAddress = WalletAddress("c")
        val redirectUrl = "https://custom-url/\${id}"

        val response = suppose("request to create asset send request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "redirect_url": "$redirectUrl",
                                "asset_type": "NATIVE",
                                "amount": "${amount.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
                                "recipient_address": "${recipientAddress.rawValue}",
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

            objectMapper.readValue(response.response.contentAsString, AssetSendRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    AssetSendRequestResponse(
                        id = response.id,
                        projectId = PROJECT_ID,
                        status = Status.PENDING,
                        chainId = PROJECT.chainId.value,
                        tokenAddress = null,
                        assetType = AssetType.NATIVE,
                        amount = amount.rawValue,
                        senderAddress = senderAddress.rawValue,
                        recipientAddress = recipientAddress.rawValue,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        redirectUrl = "https://custom-url/${response.id.value}",
                        sendTx = TransactionResponse(
                            txHash = null,
                            from = senderAddress.rawValue,
                            to = recipientAddress.rawValue,
                            data = null,
                            value = amount.rawValue,
                            blockConfirmations = null,
                            timestamp = null
                        ),
                        createdAt = response.createdAt,
                        events = null
                    )
                )

            expectThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("asset send request is correctly stored in database") {
            val storedRequest = assetSendRequestRepository.getById(response.id)

            expectThat(storedRequest)
                .isEqualTo(
                    AssetSendRequest(
                        id = response.id,
                        projectId = PROJECT_ID,
                        chainId = PROJECT.chainId,
                        redirectUrl = "https://custom-url/${response.id.value}",
                        tokenAddress = null,
                        assetAmount = amount,
                        assetSenderAddress = senderAddress,
                        assetRecipientAddress = recipientAddress,
                        txHash = null,
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
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress("b")
        val recipientAddress = WalletAddress("c")

        verify("400 is returned for missing token address") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "asset_type": "TOKEN",
                                "amount": "${amount.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
                                "recipient_address": "${recipientAddress.rawValue}",
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
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress("b")
        val recipientAddress = WalletAddress("c")

        verify("400 is returned for non-allowed token address") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "asset_type": "NATIVE",
                                "amount": "${amount.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
                                "recipient_address": "${recipientAddress.rawValue}",
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
    fun mustReturn401UnauthorizedWhenCreatingAssetSendRequestWithInvalidApiKey() {
        val tokenAddress = ContractAddress("cafebabe")
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress("b")
        val recipientAddress = WalletAddress("c")

        verify("401 is returned for invalid API key") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
                    .header(CustomHeaders.API_KEY_HEADER, "invalid-api-key")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "amount": "${amount.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
                                "recipient_address": "${recipientAddress.rawValue}",
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
    fun mustCorrectlyFetchAssetSendRequestForSomeToken() {
        val mainAccount = accounts[0]

        val contract = suppose("simple asset contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(mainAccount.address),
                listOf(BigInteger("10000")),
                mainAccount.address
            ).send()
        }

        val tokenAddress = ContractAddress(contract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val createResponse = suppose("request to create asset send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "asset_type": "TOKEN",
                                "amount": "${amount.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
                                "recipient_address": "${recipientAddress.rawValue}",
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

            objectMapper.readValue(createResponse.response.contentAsString, AssetSendRequestResponse::class.java)
        }

        val txHash = suppose("some asset transfer transaction is made") {
            contract.transfer(recipientAddress.rawValue, amount.rawValue).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("transaction info is attached to asset send request") {
            assetSendRequestRepository.setTxInfo(createResponse.id, txHash, senderAddress)
        }

        val fetchResponse = suppose("request to fetch asset send request is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/send/${createResponse.id.value}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, AssetSendRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    AssetSendRequestResponse(
                        id = createResponse.id,
                        projectId = PROJECT_ID,
                        status = Status.SUCCESS,
                        chainId = PROJECT.chainId.value,
                        tokenAddress = tokenAddress.rawValue,
                        assetType = AssetType.TOKEN,
                        amount = amount.rawValue,
                        senderAddress = senderAddress.rawValue,
                        recipientAddress = recipientAddress.rawValue,
                        arbitraryData = createResponse.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-send/${createResponse.id.value}/action",
                        sendTx = TransactionResponse(
                            txHash = txHash.value,
                            from = senderAddress.rawValue,
                            to = tokenAddress.rawValue,
                            data = createResponse.sendTx.data,
                            value = BigInteger.ZERO,
                            blockConfirmations = fetchResponse.sendTx.blockConfirmations,
                            timestamp = fetchResponse.sendTx.timestamp
                        ),
                        createdAt = fetchResponse.createdAt,
                        events = EVENTS
                    )
                )

            expectThat(fetchResponse.sendTx.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.sendTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchAssetSendRequestForSomeTokenWhenCustomRpcUrlIsSpecified() {
        val mainAccount = accounts[0]

        val contract = suppose("simple asset contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(mainAccount.address),
                listOf(BigInteger("10000")),
                mainAccount.address
            ).send()
        }

        val tokenAddress = ContractAddress(contract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val (projectId, chainId, apiKey) = suppose("project with customRpcUrl is inserted into database") {
            insertProjectWithCustomRpcUrl()
        }

        val createResponse = suppose("request to create asset send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
                    .header(CustomHeaders.API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "asset_type": "TOKEN",
                                "amount": "${amount.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
                                "recipient_address": "${recipientAddress.rawValue}",
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

            objectMapper.readValue(createResponse.response.contentAsString, AssetSendRequestResponse::class.java)
        }

        val txHash = suppose("some asset transfer transaction is made") {
            contract.transfer(recipientAddress.rawValue, amount.rawValue).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("transaction info is attached to asset send request") {
            assetSendRequestRepository.setTxInfo(createResponse.id, txHash, senderAddress)
        }

        val fetchResponse = suppose("request to fetch asset send request is made") {
            val fetchResponse = mockMvc.perform(MockMvcRequestBuilders.get("/v1/send/${createResponse.id.value}"))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, AssetSendRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    AssetSendRequestResponse(
                        id = createResponse.id,
                        projectId = projectId,
                        status = Status.SUCCESS,
                        chainId = chainId.value,
                        tokenAddress = tokenAddress.rawValue,
                        assetType = AssetType.TOKEN,
                        amount = amount.rawValue,
                        senderAddress = senderAddress.rawValue,
                        recipientAddress = recipientAddress.rawValue,
                        arbitraryData = createResponse.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-send/${createResponse.id.value}/action",
                        sendTx = TransactionResponse(
                            txHash = txHash.value,
                            from = senderAddress.rawValue,
                            to = tokenAddress.rawValue,
                            data = createResponse.sendTx.data,
                            value = BigInteger.ZERO,
                            blockConfirmations = fetchResponse.sendTx.blockConfirmations,
                            timestamp = fetchResponse.sendTx.timestamp
                        ),
                        createdAt = fetchResponse.createdAt,
                        events = EVENTS
                    )
                )

            expectThat(fetchResponse.sendTx.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.sendTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchAssetSendRequestForNativeAsset() {
        val mainAccount = accounts[0]
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress("cafebafe")

        val createResponse = suppose("request to create asset send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "asset_type": "NATIVE",
                                "amount": "${amount.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
                                "recipient_address": "${recipientAddress.rawValue}",
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

            objectMapper.readValue(createResponse.response.contentAsString, AssetSendRequestResponse::class.java)
        }

        val txHash = suppose("some regular transfer transaction is made") {
            Transfer.sendFunds(
                hardhatContainer.web3j,
                mainAccount,
                recipientAddress.rawValue,
                amount.rawValue.toBigDecimal(),
                Convert.Unit.WEI
            ).send()?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("transaction info is attached to asset send request") {
            assetSendRequestRepository.setTxInfo(createResponse.id, txHash, senderAddress)
        }

        val fetchResponse = suppose("request to fetch asset send request is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/send/${createResponse.id.value}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, AssetSendRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    AssetSendRequestResponse(
                        id = createResponse.id,
                        projectId = PROJECT_ID,
                        status = Status.SUCCESS,
                        chainId = PROJECT.chainId.value,
                        tokenAddress = null,
                        assetType = AssetType.NATIVE,
                        amount = amount.rawValue,
                        senderAddress = senderAddress.rawValue,
                        recipientAddress = recipientAddress.rawValue,
                        arbitraryData = createResponse.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-send/${createResponse.id.value}/action",
                        sendTx = TransactionResponse(
                            txHash = txHash.value,
                            from = senderAddress.rawValue,
                            to = recipientAddress.rawValue,
                            data = null,
                            value = amount.rawValue,
                            blockConfirmations = fetchResponse.sendTx.blockConfirmations,
                            timestamp = fetchResponse.sendTx.timestamp
                        ),
                        createdAt = fetchResponse.createdAt,
                        events = emptyList()
                    )
                )

            expectThat(fetchResponse.sendTx.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.sendTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchAssetSendRequestForNativeAssetWhenCustomRpcUrlIsSpecified() {
        val mainAccount = accounts[0]
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress("cafebabe")

        val (projectId, chainId, apiKey) = suppose("project with customRpcUrl is inserted into database") {
            insertProjectWithCustomRpcUrl()
        }

        val createResponse = suppose("request to create asset send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
                    .header(CustomHeaders.API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "asset_type": "NATIVE",
                                "amount": "${amount.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
                                "recipient_address": "${recipientAddress.rawValue}",
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

            objectMapper.readValue(createResponse.response.contentAsString, AssetSendRequestResponse::class.java)
        }

        val txHash = suppose("some regular transfer transaction is made") {
            Transfer.sendFunds(
                hardhatContainer.web3j,
                mainAccount,
                recipientAddress.rawValue,
                amount.rawValue.toBigDecimal(),
                Convert.Unit.WEI
            ).send()?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("transaction info is attached to asset send request") {
            assetSendRequestRepository.setTxInfo(createResponse.id, txHash, senderAddress)
        }

        val fetchResponse = suppose("request to fetch asset send request is made") {
            val fetchResponse = mockMvc.perform(MockMvcRequestBuilders.get("/v1/send/${createResponse.id.value}"))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, AssetSendRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    AssetSendRequestResponse(
                        id = createResponse.id,
                        projectId = projectId,
                        status = Status.SUCCESS,
                        chainId = chainId.value,
                        tokenAddress = null,
                        assetType = AssetType.NATIVE,
                        amount = amount.rawValue,
                        senderAddress = senderAddress.rawValue,
                        recipientAddress = recipientAddress.rawValue,
                        arbitraryData = createResponse.arbitraryData,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = "before-action-message",
                            afterActionMessage = "after-action-message"
                        ),
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-send/${createResponse.id.value}/action",
                        sendTx = TransactionResponse(
                            txHash = txHash.value,
                            from = senderAddress.rawValue,
                            to = recipientAddress.rawValue,
                            data = null,
                            value = amount.rawValue,
                            blockConfirmations = fetchResponse.sendTx.blockConfirmations,
                            timestamp = fetchResponse.sendTx.timestamp
                        ),
                        createdAt = fetchResponse.createdAt,
                        events = emptyList()
                    )
                )

            expectThat(fetchResponse.sendTx.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.sendTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustReturn404NotFoundForNonExistentAssetSendRequest() {
        verify("404 is returned for non-existent asset send request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/send/${UUID.randomUUID()}")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustCorrectlyFetchAssetSendRequestsByProjectId() {
        val mainAccount = accounts[0]

        val contract = suppose("simple asset contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(mainAccount.address),
                listOf(BigInteger("10000")),
                mainAccount.address
            ).send()
        }

        val tokenAddress = ContractAddress(contract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val createResponse = suppose("request to create asset send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "asset_type": "TOKEN",
                                "amount": "${amount.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
                                "recipient_address": "${recipientAddress.rawValue}",
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

            objectMapper.readValue(createResponse.response.contentAsString, AssetSendRequestResponse::class.java)
        }

        val txHash = suppose("some asset transfer transaction is made") {
            contract.transfer(recipientAddress.rawValue, amount.rawValue).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("transaction info is attached to asset send request") {
            assetSendRequestRepository.setTxInfo(createResponse.id, txHash, senderAddress)
        }

        val fetchResponse = suppose("request to fetch asset send requests by project ID is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/send/by-project/${createResponse.projectId.value}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, AssetSendRequestsResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    AssetSendRequestsResponse(
                        listOf(
                            AssetSendRequestResponse(
                                id = createResponse.id,
                                projectId = PROJECT_ID,
                                status = Status.SUCCESS,
                                chainId = PROJECT.chainId.value,
                                tokenAddress = tokenAddress.rawValue,
                                assetType = AssetType.TOKEN,
                                amount = amount.rawValue,
                                senderAddress = senderAddress.rawValue,
                                recipientAddress = recipientAddress.rawValue,
                                arbitraryData = createResponse.arbitraryData,
                                screenConfig = ScreenConfig(
                                    beforeActionMessage = "before-action-message",
                                    afterActionMessage = "after-action-message"
                                ),
                                redirectUrl = PROJECT.baseRedirectUrl.value +
                                    "/request-send/${createResponse.id.value}/action",
                                sendTx = TransactionResponse(
                                    txHash = txHash.value,
                                    from = senderAddress.rawValue,
                                    to = tokenAddress.rawValue,
                                    data = createResponse.sendTx.data,
                                    value = BigInteger.ZERO,
                                    blockConfirmations = fetchResponse.requests[0].sendTx.blockConfirmations,
                                    timestamp = fetchResponse.requests[0].sendTx.timestamp
                                ),
                                createdAt = fetchResponse.requests[0].createdAt,
                                events = EVENTS
                            )
                        )
                    )
                )

            expectThat(fetchResponse.requests[0].sendTx.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.requests[0].sendTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.requests[0].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchAssetSendRequestsByProjectIdWhenCustomRpcUrlIsSpecified() {
        val mainAccount = accounts[0]

        val contract = suppose("simple asset contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(mainAccount.address),
                listOf(BigInteger("10000")),
                mainAccount.address
            ).send()
        }

        val redirectUrl = "https://example.com/\${id}"
        val tokenAddress = ContractAddress(contract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val (projectId, chainId, apiKey) = suppose("project with customRpcUrl is inserted into database") {
            insertProjectWithCustomRpcUrl()
        }

        val createResponse = suppose("request to create asset send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
                    .header(CustomHeaders.API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "redirect_url": "$redirectUrl",
                                "token_address": "${tokenAddress.rawValue}",
                                "asset_type": "TOKEN",
                                "amount": "${amount.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
                                "recipient_address": "${recipientAddress.rawValue}",
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

            objectMapper.readValue(createResponse.response.contentAsString, AssetSendRequestResponse::class.java)
        }

        val txHash = suppose("some asset transfer transaction is made") {
            contract.transfer(recipientAddress.rawValue, amount.rawValue).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("transaction info is attached to asset send request") {
            assetSendRequestRepository.setTxInfo(createResponse.id, txHash, senderAddress)
        }

        val fetchResponse = suppose("request to fetch asset send requests by project ID is made") {
            val fetchResponse = mockMvc.perform(MockMvcRequestBuilders.get("/v1/send/by-project/${projectId.value}"))
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, AssetSendRequestsResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    AssetSendRequestsResponse(
                        listOf(
                            AssetSendRequestResponse(
                                id = createResponse.id,
                                projectId = projectId,
                                status = Status.SUCCESS,
                                chainId = chainId.value,
                                tokenAddress = tokenAddress.rawValue,
                                assetType = AssetType.TOKEN,
                                amount = amount.rawValue,
                                senderAddress = senderAddress.rawValue,
                                recipientAddress = recipientAddress.rawValue,
                                arbitraryData = createResponse.arbitraryData,
                                screenConfig = ScreenConfig(
                                    beforeActionMessage = "before-action-message",
                                    afterActionMessage = "after-action-message"
                                ),
                                redirectUrl = redirectUrl.replace("\${id}", createResponse.id.value.toString()),
                                sendTx = TransactionResponse(
                                    txHash = txHash.value,
                                    from = senderAddress.rawValue,
                                    to = tokenAddress.rawValue,
                                    data = createResponse.sendTx.data,
                                    value = BigInteger.ZERO,
                                    blockConfirmations = fetchResponse.requests[0].sendTx.blockConfirmations,
                                    timestamp = fetchResponse.requests[0].sendTx.timestamp
                                ),
                                createdAt = fetchResponse.requests[0].createdAt,
                                events = EVENTS
                            )
                        )
                    )
                )

            expectThat(fetchResponse.requests[0].sendTx.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.requests[0].sendTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.requests[0].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchAssetSendRequestsBySenderAddress() {
        val mainAccount = accounts[0]

        val contract = suppose("simple asset contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(mainAccount.address),
                listOf(BigInteger("10000")),
                mainAccount.address
            ).send()
        }

        val tokenAddress = ContractAddress(contract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val createResponse = suppose("request to create asset send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "asset_type": "TOKEN",
                                "amount": "${amount.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
                                "recipient_address": "${recipientAddress.rawValue}",
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

            objectMapper.readValue(createResponse.response.contentAsString, AssetSendRequestResponse::class.java)
        }

        val txHash = suppose("some asset transfer transaction is made") {
            contract.transfer(recipientAddress.rawValue, amount.rawValue).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("transaction info is attached to asset send request") {
            assetSendRequestRepository.setTxInfo(createResponse.id, txHash, senderAddress)
        }

        val fetchResponse = suppose("request to fetch asset send requests by sender address is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/send/by-sender/${createResponse.senderAddress}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, AssetSendRequestsResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    AssetSendRequestsResponse(
                        listOf(
                            AssetSendRequestResponse(
                                id = createResponse.id,
                                projectId = PROJECT_ID,
                                status = Status.SUCCESS,
                                chainId = PROJECT.chainId.value,
                                tokenAddress = tokenAddress.rawValue,
                                assetType = AssetType.TOKEN,
                                amount = amount.rawValue,
                                senderAddress = senderAddress.rawValue,
                                recipientAddress = recipientAddress.rawValue,
                                arbitraryData = createResponse.arbitraryData,
                                screenConfig = ScreenConfig(
                                    beforeActionMessage = "before-action-message",
                                    afterActionMessage = "after-action-message"
                                ),
                                redirectUrl = PROJECT.baseRedirectUrl.value +
                                    "/request-send/${createResponse.id.value}/action",
                                sendTx = TransactionResponse(
                                    txHash = txHash.value,
                                    from = senderAddress.rawValue,
                                    to = tokenAddress.rawValue,
                                    data = createResponse.sendTx.data,
                                    value = BigInteger.ZERO,
                                    blockConfirmations = fetchResponse.requests[0].sendTx.blockConfirmations,
                                    timestamp = fetchResponse.requests[0].sendTx.timestamp
                                ),
                                createdAt = fetchResponse.requests[0].createdAt,
                                events = EVENTS
                            )
                        )
                    )
                )

            expectThat(fetchResponse.requests[0].sendTx.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.requests[0].sendTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.requests[0].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchAssetSendRequestsBySenderAddressWhenCustomRpcUrlIsSpecified() {
        val mainAccount = accounts[0]

        val contract = suppose("simple asset contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(mainAccount.address),
                listOf(BigInteger("10000")),
                mainAccount.address
            ).send()
        }

        val redirectUrl = "https://example.com/\${id}"
        val tokenAddress = ContractAddress(contract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val (projectId, chainId, apiKey) = suppose("project with customRpcUrl is inserted into database") {
            insertProjectWithCustomRpcUrl()
        }

        val createResponse = suppose("request to create asset send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
                    .header(CustomHeaders.API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "redirect_url": "$redirectUrl",
                                "token_address": "${tokenAddress.rawValue}",
                                "asset_type": "TOKEN",
                                "amount": "${amount.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
                                "recipient_address": "${recipientAddress.rawValue}",
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

            objectMapper.readValue(createResponse.response.contentAsString, AssetSendRequestResponse::class.java)
        }

        val txHash = suppose("some asset transfer transaction is made") {
            contract.transfer(recipientAddress.rawValue, amount.rawValue).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("transaction info is attached to asset send request") {
            assetSendRequestRepository.setTxInfo(createResponse.id, txHash, senderAddress)
        }

        val fetchResponse = suppose("request to fetch asset send requests by sender address is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/send/by-sender/${createResponse.senderAddress}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, AssetSendRequestsResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    AssetSendRequestsResponse(
                        listOf(
                            AssetSendRequestResponse(
                                id = createResponse.id,
                                projectId = projectId,
                                status = Status.SUCCESS,
                                chainId = chainId.value,
                                tokenAddress = tokenAddress.rawValue,
                                assetType = AssetType.TOKEN,
                                amount = amount.rawValue,
                                senderAddress = senderAddress.rawValue,
                                recipientAddress = recipientAddress.rawValue,
                                arbitraryData = createResponse.arbitraryData,
                                screenConfig = ScreenConfig(
                                    beforeActionMessage = "before-action-message",
                                    afterActionMessage = "after-action-message"
                                ),
                                redirectUrl = redirectUrl.replace("\${id}", createResponse.id.value.toString()),
                                sendTx = TransactionResponse(
                                    txHash = txHash.value,
                                    from = senderAddress.rawValue,
                                    to = tokenAddress.rawValue,
                                    data = createResponse.sendTx.data,
                                    value = BigInteger.ZERO,
                                    blockConfirmations = fetchResponse.requests[0].sendTx.blockConfirmations,
                                    timestamp = fetchResponse.requests[0].sendTx.timestamp
                                ),
                                createdAt = fetchResponse.requests[0].createdAt,
                                events = EVENTS
                            )
                        )
                    )
                )

            expectThat(fetchResponse.requests[0].sendTx.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.requests[0].sendTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.requests[0].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchAssetSendRequestsByRecipientAddress() {
        val mainAccount = accounts[0]

        val contract = suppose("simple asset contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(mainAccount.address),
                listOf(BigInteger("10000")),
                mainAccount.address
            ).send()
        }

        val tokenAddress = ContractAddress(contract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val createResponse = suppose("request to create asset send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "asset_type": "TOKEN",
                                "amount": "${amount.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
                                "recipient_address": "${recipientAddress.rawValue}",
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

            objectMapper.readValue(createResponse.response.contentAsString, AssetSendRequestResponse::class.java)
        }

        val txHash = suppose("some asset transfer transaction is made") {
            contract.transfer(recipientAddress.rawValue, amount.rawValue).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("transaction info is attached to asset send request") {
            assetSendRequestRepository.setTxInfo(createResponse.id, txHash, senderAddress)
        }

        val fetchResponse = suppose("request to fetch asset send requests by recipient address is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/send/by-recipient/${createResponse.recipientAddress}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, AssetSendRequestsResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    AssetSendRequestsResponse(
                        listOf(
                            AssetSendRequestResponse(
                                id = createResponse.id,
                                projectId = PROJECT_ID,
                                status = Status.SUCCESS,
                                chainId = PROJECT.chainId.value,
                                tokenAddress = tokenAddress.rawValue,
                                assetType = AssetType.TOKEN,
                                amount = amount.rawValue,
                                senderAddress = senderAddress.rawValue,
                                recipientAddress = recipientAddress.rawValue,
                                arbitraryData = createResponse.arbitraryData,
                                screenConfig = ScreenConfig(
                                    beforeActionMessage = "before-action-message",
                                    afterActionMessage = "after-action-message"
                                ),
                                redirectUrl = PROJECT.baseRedirectUrl.value +
                                    "/request-send/${createResponse.id.value}/action",
                                sendTx = TransactionResponse(
                                    txHash = txHash.value,
                                    from = senderAddress.rawValue,
                                    to = tokenAddress.rawValue,
                                    data = createResponse.sendTx.data,
                                    value = BigInteger.ZERO,
                                    blockConfirmations = fetchResponse.requests[0].sendTx.blockConfirmations,
                                    timestamp = fetchResponse.requests[0].sendTx.timestamp
                                ),
                                createdAt = fetchResponse.requests[0].createdAt,
                                events = EVENTS
                            )
                        )
                    )
                )

            expectThat(fetchResponse.requests[0].sendTx.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.requests[0].sendTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.requests[0].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchAssetSendRequestsByRecipientAddressWhenCustomRpcUrlIsSpecified() {
        val mainAccount = accounts[0]

        val contract = suppose("simple asset contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(mainAccount.address),
                listOf(BigInteger("10000")),
                mainAccount.address
            ).send()
        }

        val tokenAddress = ContractAddress(contract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val (projectId, chainId, apiKey) = suppose("project with customRpcUrl is inserted into database") {
            insertProjectWithCustomRpcUrl()
        }

        val createResponse = suppose("request to create asset send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/send")
                    .header(CustomHeaders.API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "asset_type": "TOKEN",
                                "amount": "${amount.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
                                "recipient_address": "${recipientAddress.rawValue}",
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

            objectMapper.readValue(createResponse.response.contentAsString, AssetSendRequestResponse::class.java)
        }

        val txHash = suppose("some asset transfer transaction is made") {
            contract.transfer(recipientAddress.rawValue, amount.rawValue).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("transaction info is attached to asset send request") {
            assetSendRequestRepository.setTxInfo(createResponse.id, txHash, senderAddress)
        }

        val fetchResponse = suppose("request to fetch asset send requests by recipient address is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/send/by-recipient/${createResponse.recipientAddress}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, AssetSendRequestsResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    AssetSendRequestsResponse(
                        listOf(
                            AssetSendRequestResponse(
                                id = createResponse.id,
                                projectId = projectId,
                                status = Status.SUCCESS,
                                chainId = chainId.value,
                                tokenAddress = tokenAddress.rawValue,
                                assetType = AssetType.TOKEN,
                                amount = amount.rawValue,
                                senderAddress = senderAddress.rawValue,
                                recipientAddress = recipientAddress.rawValue,
                                arbitraryData = createResponse.arbitraryData,
                                screenConfig = ScreenConfig(
                                    beforeActionMessage = "before-action-message",
                                    afterActionMessage = "after-action-message"
                                ),
                                redirectUrl = PROJECT.baseRedirectUrl.value +
                                    "/request-send/${createResponse.id.value}/action",
                                sendTx = TransactionResponse(
                                    txHash = txHash.value,
                                    from = senderAddress.rawValue,
                                    to = tokenAddress.rawValue,
                                    data = createResponse.sendTx.data,
                                    value = BigInteger.ZERO,
                                    blockConfirmations = fetchResponse.requests[0].sendTx.blockConfirmations,
                                    timestamp = fetchResponse.requests[0].sendTx.timestamp
                                ),
                                createdAt = fetchResponse.requests[0].createdAt,
                                events = EVENTS
                            )
                        )
                    )
                )

            expectThat(fetchResponse.requests[0].sendTx.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.requests[0].sendTx.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.requests[0].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyAttachTransactionInfo() {
        val id = AssetSendRequestId(UUID.randomUUID())
        val tokenSender = WalletAddress("b")

        suppose("some asset send request without transaction info exists in database") {
            assetSendRequestRepository.store(
                StoreAssetSendRequestParams(
                    id = id,
                    projectId = PROJECT_ID,
                    chainId = TestData.CHAIN_ID,
                    redirectUrl = "https://example.com/${id.value}",
                    tokenAddress = ContractAddress("a"),
                    assetAmount = Balance(BigInteger.TEN),
                    assetSenderAddress = tokenSender,
                    assetRecipientAddress = WalletAddress("c"),
                    arbitraryData = TestData.EMPTY_JSON_OBJECT,
                    screenConfig = ScreenConfig(
                        beforeActionMessage = "before-action-message",
                        afterActionMessage = "after-action-message"
                    ),
                    createdAt = TestData.TIMESTAMP
                )
            )
        }

        val txHash = TransactionHash("0x1")

        suppose("request to attach transaction info to asset send request is made") {
            mockMvc.perform(
                MockMvcRequestBuilders.put("/v1/send/${id.value}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "tx_hash": "${txHash.value}",
                                "caller_address": "${tokenSender.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        }

        verify("transaction info is correctly attached to asset send request") {
            val storedRequest = assetSendRequestRepository.getById(id)

            expectThat(storedRequest?.txHash)
                .isEqualTo(txHash)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenTransactionInfoIsNotAttached() {
        val id = AssetSendRequestId(UUID.randomUUID())
        val txHash = TransactionHash("0x1")
        val tokenSender = WalletAddress("b")

        suppose("some asset send request with transaction info exists in database") {
            assetSendRequestRepository.store(
                StoreAssetSendRequestParams(
                    id = id,
                    projectId = PROJECT_ID,
                    chainId = TestData.CHAIN_ID,
                    redirectUrl = "https://example.com/${id.value}",
                    tokenAddress = ContractAddress("a"),
                    assetAmount = Balance(BigInteger.TEN),
                    assetSenderAddress = tokenSender,
                    assetRecipientAddress = WalletAddress("c"),
                    arbitraryData = TestData.EMPTY_JSON_OBJECT,
                    screenConfig = ScreenConfig(
                        beforeActionMessage = "before-action-message",
                        afterActionMessage = "after-action-message"
                    ),
                    createdAt = TestData.TIMESTAMP
                )
            )
            assetSendRequestRepository.setTxInfo(id, txHash, tokenSender)
        }

        verify("400 is returned when attaching transaction info") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.put("/v1/send/${id.value}")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "tx_hash": "0x2",
                                "caller_address": "${tokenSender.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.TX_INFO_ALREADY_SET)
        }

        verify("transaction info is not changed in database") {
            val storedRequest = assetSendRequestRepository.getById(id)

            expectThat(storedRequest?.txHash)
                .isEqualTo(txHash)
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
