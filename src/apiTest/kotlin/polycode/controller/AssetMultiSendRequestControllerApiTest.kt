package polycode.controller

import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.tx.gas.DefaultGasProvider
import polycode.ControllerTestBase
import polycode.TestData
import polycode.blockchain.SimpleDisperse
import polycode.blockchain.SimpleERC20
import polycode.config.CustomHeaders
import polycode.exception.ErrorCode
import polycode.features.api.access.model.result.Project
import polycode.features.asset.multisend.model.params.StoreAssetMultiSendRequestParams
import polycode.features.asset.multisend.model.response.AssetMultiSendRequestResponse
import polycode.features.asset.multisend.model.response.AssetMultiSendRequestsResponse
import polycode.features.asset.multisend.model.response.MultiSendItemResponse
import polycode.features.asset.multisend.model.result.AssetMultiSendRequest
import polycode.features.asset.multisend.repository.AssetMultiSendRequestRepository
import polycode.generated.jooq.enums.UserIdentifierType
import polycode.generated.jooq.id.ApiKeyId
import polycode.generated.jooq.id.AssetMultiSendRequestId
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

class AssetMultiSendRequestControllerApiTest : ControllerTestBase() {

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
        private val APPROVE_EVENTS = listOf(
            EventInfoResponse(
                signature = "Approval(address,address,uint256)",
                arguments = listOf(
                    EventArgumentResponse(
                        name = "owner",
                        type = EventArgumentResponseType.VALUE,
                        value = HardhatTestContainer.ACCOUNT_ADDRESS_1,
                        hash = null
                    ),
                    EventArgumentResponse(
                        name = "spender",
                        type = EventArgumentResponseType.VALUE,
                        value = "{disperseContractAddress}",
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
        private val DISPERSE_EVENTS = listOf(
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
                        value = "{disperseContractAddress}",
                        hash = null
                    ),
                    EventArgumentResponse(
                        name = "value",
                        type = EventArgumentResponseType.VALUE,
                        value = "10",
                        hash = null
                    )
                )
            ),
            EventInfoResponse(
                signature = "Transfer(address,address,uint256)",
                arguments = listOf(
                    EventArgumentResponse(
                        name = "from",
                        type = EventArgumentResponseType.VALUE,
                        value = "{disperseContractAddress}",
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
    private lateinit var assetMultiSendRequestRepository: AssetMultiSendRequestRepository

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
    fun mustCorrectlyCreateAssetMultiSendRequestForSomeToken() {
        val tokenAddress = ContractAddress("cafebabe")
        val disperseContractAddress = ContractAddress("abe")
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress("b")
        val recipientAddress = WalletAddress("c")

        val response = suppose("request to create asset multi-send request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/multi-send")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "disperse_contract_address": "${disperseContractAddress.rawValue}",
                                "asset_type": "TOKEN",
                                "items": [
                                    {
                                        "wallet_address": "${recipientAddress.rawValue}",
                                        "amount": "${amount.rawValue}",
                                        "item_name": "Example"
                                    }
                                ],
                                "sender_address": "${senderAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "approve_screen_config": {
                                    "before_action_message": "approve-before-action-message",
                                    "after_action_message": "approve-after-action-message"
                                },
                                "disperse_screen_config": {
                                    "before_action_message": "disperse-before-action-message",
                                    "after_action_message": "disperse-after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, AssetMultiSendRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    AssetMultiSendRequestResponse(
                        id = response.id,
                        projectId = PROJECT_ID,
                        approveStatus = Status.PENDING,
                        disperseStatus = null,
                        chainId = PROJECT.chainId.value,
                        tokenAddress = tokenAddress.rawValue,
                        disperseContractAddress = disperseContractAddress.rawValue,
                        assetType = AssetType.TOKEN,
                        items = listOf(
                            MultiSendItemResponse(
                                walletAddress = recipientAddress.rawValue,
                                amount = amount.rawValue,
                                itemName = "Example"
                            )
                        ),
                        senderAddress = senderAddress.rawValue,
                        arbitraryData = response.arbitraryData,
                        approveScreenConfig = ScreenConfig(
                            beforeActionMessage = "approve-before-action-message",
                            afterActionMessage = "approve-after-action-message"
                        ),
                        disperseScreenConfig = ScreenConfig(
                            beforeActionMessage = "disperse-before-action-message",
                            afterActionMessage = "disperse-after-action-message"
                        ),
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-multi-send/${response.id.value}/action",
                        approveTx = TransactionResponse(
                            txHash = null,
                            from = senderAddress.rawValue,
                            to = tokenAddress.rawValue,
                            data = response.approveTx!!.data,
                            value = BigInteger.ZERO,
                            blockConfirmations = null,
                            timestamp = null
                        ),
                        disperseTx = null,
                        createdAt = response.createdAt,
                        approveEvents = null,
                        disperseEvents = null
                    )
                )

            expectThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("asset multi-send request is correctly stored in database") {
            val storedRequest = assetMultiSendRequestRepository.getById(response.id)

            expectThat(storedRequest)
                .isEqualTo(
                    AssetMultiSendRequest(
                        id = response.id,
                        projectId = PROJECT_ID,
                        chainId = PROJECT.chainId,
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-multi-send/${response.id.value}/action",
                        tokenAddress = tokenAddress,
                        disperseContractAddress = disperseContractAddress,
                        assetAmounts = listOf(amount),
                        assetRecipientAddresses = listOf(recipientAddress),
                        itemNames = listOf("Example"),
                        assetSenderAddress = senderAddress,
                        approveTxHash = null,
                        disperseTxHash = null,
                        arbitraryData = response.arbitraryData,
                        approveScreenConfig = ScreenConfig(
                            beforeActionMessage = "approve-before-action-message",
                            afterActionMessage = "approve-after-action-message"
                        ),
                        disperseScreenConfig = ScreenConfig(
                            beforeActionMessage = "disperse-before-action-message",
                            afterActionMessage = "disperse-after-action-message"
                        ),
                        createdAt = storedRequest!!.createdAt
                    )
                )

            expectThat(storedRequest.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyCreateAssetMultiSendRequestForSomeTokenWithRedirectUrl() {
        val tokenAddress = ContractAddress("cafebabe")
        val disperseContractAddress = ContractAddress("abe")
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress("b")
        val recipientAddress = WalletAddress("c")
        val redirectUrl = "https://custom-url/\${id}"

        val response = suppose("request to create asset multi-send request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/multi-send")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "redirect_url": "$redirectUrl",
                                "token_address": "${tokenAddress.rawValue}",
                                "disperse_contract_address": "${disperseContractAddress.rawValue}",
                                "asset_type": "TOKEN",
                                "items": [
                                    {
                                        "wallet_address": "${recipientAddress.rawValue}",
                                        "amount": "${amount.rawValue}",
                                        "item_name": "Example"
                                    }
                                ],
                                "sender_address": "${senderAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "approve_screen_config": {
                                    "before_action_message": "approve-before-action-message",
                                    "after_action_message": "approve-after-action-message"
                                },
                                "disperse_screen_config": {
                                    "before_action_message": "disperse-before-action-message",
                                    "after_action_message": "disperse-after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, AssetMultiSendRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    AssetMultiSendRequestResponse(
                        id = response.id,
                        projectId = PROJECT_ID,
                        approveStatus = Status.PENDING,
                        disperseStatus = null,
                        chainId = PROJECT.chainId.value,
                        tokenAddress = tokenAddress.rawValue,
                        disperseContractAddress = disperseContractAddress.rawValue,
                        assetType = AssetType.TOKEN,
                        items = listOf(
                            MultiSendItemResponse(
                                walletAddress = recipientAddress.rawValue,
                                amount = amount.rawValue,
                                itemName = "Example"
                            )
                        ),
                        senderAddress = senderAddress.rawValue,
                        arbitraryData = response.arbitraryData,
                        approveScreenConfig = ScreenConfig(
                            beforeActionMessage = "approve-before-action-message",
                            afterActionMessage = "approve-after-action-message"
                        ),
                        disperseScreenConfig = ScreenConfig(
                            beforeActionMessage = "disperse-before-action-message",
                            afterActionMessage = "disperse-after-action-message"
                        ),
                        redirectUrl = "https://custom-url/${response.id.value}",
                        approveTx = TransactionResponse(
                            txHash = null,
                            from = senderAddress.rawValue,
                            to = tokenAddress.rawValue,
                            data = response.approveTx!!.data,
                            value = BigInteger.ZERO,
                            blockConfirmations = null,
                            timestamp = null
                        ),
                        disperseTx = null,
                        createdAt = response.createdAt,
                        approveEvents = null,
                        disperseEvents = null
                    )
                )

            expectThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("asset multi-send request is correctly stored in database") {
            val storedRequest = assetMultiSendRequestRepository.getById(response.id)

            expectThat(storedRequest)
                .isEqualTo(
                    AssetMultiSendRequest(
                        id = response.id,
                        projectId = PROJECT_ID,
                        chainId = PROJECT.chainId,
                        redirectUrl = "https://custom-url/${response.id.value}",
                        tokenAddress = tokenAddress,
                        disperseContractAddress = disperseContractAddress,
                        assetAmounts = listOf(amount),
                        assetRecipientAddresses = listOf(recipientAddress),
                        itemNames = listOf("Example"),
                        assetSenderAddress = senderAddress,
                        approveTxHash = null,
                        disperseTxHash = null,
                        arbitraryData = response.arbitraryData,
                        approveScreenConfig = ScreenConfig(
                            beforeActionMessage = "approve-before-action-message",
                            afterActionMessage = "approve-after-action-message"
                        ),
                        disperseScreenConfig = ScreenConfig(
                            beforeActionMessage = "disperse-before-action-message",
                            afterActionMessage = "disperse-after-action-message"
                        ),
                        createdAt = storedRequest!!.createdAt
                    )
                )

            expectThat(storedRequest.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyCreateAssetMultiSendRequestForNativeAsset() {
        val disperseContractAddress = ContractAddress("abe")
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress("b")
        val recipientAddress = WalletAddress("c")

        val response = suppose("request to create asset multi-send request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/multi-send")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "disperse_contract_address": "${disperseContractAddress.rawValue}",
                                "asset_type": "NATIVE",
                                "items": [
                                    {
                                        "wallet_address": "${recipientAddress.rawValue}",
                                        "amount": "${amount.rawValue}",
                                        "item_name": "Example"
                                    }
                                ],
                                "sender_address": "${senderAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "approve_screen_config": {
                                    "before_action_message": "approve-before-action-message",
                                    "after_action_message": "approve-after-action-message"
                                },
                                "disperse_screen_config": {
                                    "before_action_message": "disperse-before-action-message",
                                    "after_action_message": "disperse-after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, AssetMultiSendRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    AssetMultiSendRequestResponse(
                        id = response.id,
                        projectId = PROJECT_ID,
                        approveStatus = null,
                        disperseStatus = Status.PENDING,
                        chainId = PROJECT.chainId.value,
                        tokenAddress = null,
                        disperseContractAddress = disperseContractAddress.rawValue,
                        assetType = AssetType.NATIVE,
                        items = listOf(
                            MultiSendItemResponse(
                                walletAddress = recipientAddress.rawValue,
                                amount = amount.rawValue,
                                itemName = "Example"
                            )
                        ),
                        senderAddress = senderAddress.rawValue,
                        arbitraryData = response.arbitraryData,
                        approveScreenConfig = ScreenConfig(
                            beforeActionMessage = "approve-before-action-message",
                            afterActionMessage = "approve-after-action-message"
                        ),
                        disperseScreenConfig = ScreenConfig(
                            beforeActionMessage = "disperse-before-action-message",
                            afterActionMessage = "disperse-after-action-message"
                        ),
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-multi-send/${response.id.value}/action",
                        approveTx = null,
                        disperseTx = TransactionResponse(
                            txHash = null,
                            from = senderAddress.rawValue,
                            to = disperseContractAddress.rawValue,
                            data = response.disperseTx!!.data,
                            value = amount.rawValue,
                            blockConfirmations = null,
                            timestamp = null
                        ),
                        createdAt = response.createdAt,
                        approveEvents = null,
                        disperseEvents = null
                    )
                )

            expectThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("asset multi-send request is correctly stored in database") {
            val storedRequest = assetMultiSendRequestRepository.getById(response.id)

            expectThat(storedRequest)
                .isEqualTo(
                    AssetMultiSendRequest(
                        id = response.id,
                        projectId = PROJECT_ID,
                        chainId = PROJECT.chainId,
                        redirectUrl = PROJECT.baseRedirectUrl.value + "/request-multi-send/${response.id.value}/action",
                        tokenAddress = null,
                        disperseContractAddress = disperseContractAddress,
                        assetAmounts = listOf(amount),
                        assetRecipientAddresses = listOf(recipientAddress),
                        itemNames = listOf("Example"),
                        assetSenderAddress = senderAddress,
                        approveTxHash = null,
                        disperseTxHash = null,
                        arbitraryData = response.arbitraryData,
                        approveScreenConfig = ScreenConfig(
                            beforeActionMessage = "approve-before-action-message",
                            afterActionMessage = "approve-after-action-message"
                        ),
                        disperseScreenConfig = ScreenConfig(
                            beforeActionMessage = "disperse-before-action-message",
                            afterActionMessage = "disperse-after-action-message"
                        ),
                        createdAt = storedRequest!!.createdAt
                    )
                )

            expectThat(storedRequest.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyCreateAssetMultiSendRequestForNativeAssetWithRedirectUrl() {
        val disperseContractAddress = ContractAddress("abe")
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress("b")
        val recipientAddress = WalletAddress("c")
        val redirectUrl = "https://custom-url/\${id}"

        val response = suppose("request to create asset multi-send request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/multi-send")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "redirect_url": "$redirectUrl",
                                "disperse_contract_address": "${disperseContractAddress.rawValue}",
                                "asset_type": "NATIVE",
                                "items": [
                                    {
                                        "wallet_address": "${recipientAddress.rawValue}",
                                        "amount": "${amount.rawValue}",
                                        "item_name": "Example"
                                    }
                                ],
                                "sender_address": "${senderAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "approve_screen_config": {
                                    "before_action_message": "approve-before-action-message",
                                    "after_action_message": "approve-after-action-message"
                                },
                                "disperse_screen_config": {
                                    "before_action_message": "disperse-before-action-message",
                                    "after_action_message": "disperse-after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, AssetMultiSendRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    AssetMultiSendRequestResponse(
                        id = response.id,
                        projectId = PROJECT_ID,
                        approveStatus = null,
                        disperseStatus = Status.PENDING,
                        chainId = PROJECT.chainId.value,
                        tokenAddress = null,
                        disperseContractAddress = disperseContractAddress.rawValue,
                        assetType = AssetType.NATIVE,
                        items = listOf(
                            MultiSendItemResponse(
                                walletAddress = recipientAddress.rawValue,
                                amount = amount.rawValue,
                                itemName = "Example"
                            )
                        ),
                        senderAddress = senderAddress.rawValue,
                        arbitraryData = response.arbitraryData,
                        approveScreenConfig = ScreenConfig(
                            beforeActionMessage = "approve-before-action-message",
                            afterActionMessage = "approve-after-action-message"
                        ),
                        disperseScreenConfig = ScreenConfig(
                            beforeActionMessage = "disperse-before-action-message",
                            afterActionMessage = "disperse-after-action-message"
                        ),
                        redirectUrl = "https://custom-url/${response.id.value}",
                        approveTx = null,
                        disperseTx = TransactionResponse(
                            txHash = null,
                            from = senderAddress.rawValue,
                            to = disperseContractAddress.rawValue,
                            data = response.disperseTx!!.data,
                            value = amount.rawValue,
                            blockConfirmations = null,
                            timestamp = null
                        ),
                        createdAt = response.createdAt,
                        approveEvents = null,
                        disperseEvents = null
                    )
                )

            expectThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("asset multi-send request is correctly stored in database") {
            val storedRequest = assetMultiSendRequestRepository.getById(response.id)

            expectThat(storedRequest)
                .isEqualTo(
                    AssetMultiSendRequest(
                        id = response.id,
                        projectId = PROJECT_ID,
                        chainId = PROJECT.chainId,
                        redirectUrl = "https://custom-url/${response.id.value}",
                        tokenAddress = null,
                        disperseContractAddress = disperseContractAddress,
                        assetAmounts = listOf(amount),
                        assetRecipientAddresses = listOf(recipientAddress),
                        itemNames = listOf("Example"),
                        assetSenderAddress = senderAddress,
                        approveTxHash = null,
                        disperseTxHash = null,
                        arbitraryData = response.arbitraryData,
                        approveScreenConfig = ScreenConfig(
                            beforeActionMessage = "approve-before-action-message",
                            afterActionMessage = "approve-after-action-message"
                        ),
                        disperseScreenConfig = ScreenConfig(
                            beforeActionMessage = "disperse-before-action-message",
                            afterActionMessage = "disperse-after-action-message"
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
        val disperseContractAddress = ContractAddress("abe")
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress("b")
        val recipientAddress = WalletAddress("c")

        verify("400 is returned for missing token address") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/multi-send")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "disperse_contract_address": "${disperseContractAddress.rawValue}",
                                "asset_type": "TOKEN",
                                "items": [
                                    {
                                        "wallet_address": "${recipientAddress.rawValue}",
                                        "amount": "${amount.rawValue}",
                                        "item_name": "Example"
                                    }
                                ],
                                "sender_address": "${senderAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "approve_screen_config": {
                                    "before_action_message": "approve-before-action-message",
                                    "after_action_message": "approve-after-action-message"
                                },
                                "disperse_screen_config": {
                                    "before_action_message": "disperse-before-action-message",
                                    "after_action_message": "disperse-after-action-message"
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
        val disperseContractAddress = ContractAddress("abe")
        val tokenAddress = ContractAddress("cafebabe")
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress("b")
        val recipientAddress = WalletAddress("c")

        verify("400 is returned for non-allowed token address") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/multi-send")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "disperse_contract_address": "${disperseContractAddress.rawValue}",
                                "asset_type": "NATIVE",
                                "items": [
                                    {
                                        "wallet_address": "${recipientAddress.rawValue}",
                                        "amount": "${amount.rawValue}",
                                        "item_name": "Example"
                                    }
                                ],
                                "sender_address": "${senderAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "approve_screen_config": {
                                    "before_action_message": "approve-before-action-message",
                                    "after_action_message": "approve-after-action-message"
                                },
                                "disperse_screen_config": {
                                    "before_action_message": "disperse-before-action-message",
                                    "after_action_message": "disperse-after-action-message"
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
    fun mustReturn401UnauthorizedWhenCreatingAssetMultiSendRequestWithInvalidApiKey() {
        val tokenAddress = ContractAddress("cafebabe")
        val disperseContractAddress = ContractAddress("abe")
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress("b")
        val recipientAddress = WalletAddress("c")

        verify("401 is returned for invalid API key") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/multi-send")
                    .header(CustomHeaders.API_KEY_HEADER, "invalid-api-key")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "disperse_contract_address": "${disperseContractAddress.rawValue}",
                                "asset_type": "TOKEN",
                                "items": [
                                    {
                                        "wallet_address": "${recipientAddress.rawValue}",
                                        "amount": "${amount.rawValue}",
                                        "item_name": "Example"
                                    }
                                ],
                                "sender_address": "${senderAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "approve_screen_config": {
                                    "before_action_message": "approve-before-action-message",
                                    "after_action_message": "approve-after-action-message"
                                },
                                "disperse_screen_config": {
                                    "before_action_message": "disperse-before-action-message",
                                    "after_action_message": "disperse-after-action-message"
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
    fun mustCorrectlyFetchAssetMultiSendRequestForSomeToken() {
        val mainAccount = accounts[0]

        val tokenContract = suppose("simple asset contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(mainAccount.address),
                listOf(BigInteger("10000")),
                mainAccount.address
            ).send()
        }

        val disperseContract = suppose("simple disperse contract is deployed") {
            SimpleDisperse.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider()
            ).send()
        }

        val tokenAddress = ContractAddress(tokenContract.contractAddress)
        val disperseContractAddress = ContractAddress(disperseContract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val createResponse = suppose("request to create asset multi-send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/multi-send")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "disperse_contract_address": "${disperseContractAddress.rawValue}",
                                "asset_type": "TOKEN",
                                "items": [
                                    {
                                        "wallet_address": "${recipientAddress.rawValue}",
                                        "amount": "${amount.rawValue}",
                                        "item_name": "Example"
                                    }
                                ],
                                "sender_address": "${senderAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "approve_screen_config": {
                                    "before_action_message": "approve-before-action-message",
                                    "after_action_message": "approve-after-action-message"
                                },
                                "disperse_screen_config": {
                                    "before_action_message": "disperse-before-action-message",
                                    "after_action_message": "disperse-after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(createResponse.response.contentAsString, AssetMultiSendRequestResponse::class.java)
        }

        val approveTxHash = suppose("some asset approve transaction is made") {
            tokenContract.approve(disperseContractAddress.rawValue, amount.rawValue).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("approve transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("approve transaction info is attached to asset multi-send request") {
            assetMultiSendRequestRepository.setApproveTxInfo(createResponse.id, approveTxHash, senderAddress)
        }

        val disperseTxHash = suppose("some disperse transaction is made") {
            disperseContract.disperseToken(
                tokenAddress.rawValue,
                listOf(recipientAddress.rawValue),
                listOf(amount.rawValue)
            ).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("disperse transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("disperse transaction info is attached to asset multi-send request") {
            assetMultiSendRequestRepository.setDisperseTxInfo(createResponse.id, disperseTxHash, senderAddress)
        }

        val fetchResponse = suppose("request to fetch asset multi-send request is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/multi-send/${createResponse.id.value}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, AssetMultiSendRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    AssetMultiSendRequestResponse(
                        id = fetchResponse.id,
                        projectId = PROJECT_ID,
                        approveStatus = Status.SUCCESS,
                        disperseStatus = Status.SUCCESS,
                        chainId = PROJECT.chainId.value,
                        tokenAddress = tokenAddress.rawValue,
                        disperseContractAddress = disperseContractAddress.rawValue,
                        assetType = AssetType.TOKEN,
                        items = listOf(
                            MultiSendItemResponse(
                                walletAddress = recipientAddress.rawValue,
                                amount = amount.rawValue,
                                itemName = "Example"
                            )
                        ),
                        senderAddress = senderAddress.rawValue,
                        arbitraryData = fetchResponse.arbitraryData,
                        approveScreenConfig = ScreenConfig(
                            beforeActionMessage = "approve-before-action-message",
                            afterActionMessage = "approve-after-action-message"
                        ),
                        disperseScreenConfig = ScreenConfig(
                            beforeActionMessage = "disperse-before-action-message",
                            afterActionMessage = "disperse-after-action-message"
                        ),
                        redirectUrl = PROJECT.baseRedirectUrl.value +
                            "/request-multi-send/${fetchResponse.id.value}/action",
                        approveTx = TransactionResponse(
                            txHash = approveTxHash.value,
                            from = senderAddress.rawValue,
                            to = tokenAddress.rawValue,
                            data = fetchResponse.approveTx!!.data,
                            value = BigInteger.ZERO,
                            blockConfirmations = fetchResponse.approveTx!!.blockConfirmations,
                            timestamp = fetchResponse.approveTx!!.timestamp
                        ),
                        disperseTx = TransactionResponse(
                            txHash = disperseTxHash.value,
                            from = senderAddress.rawValue,
                            to = disperseContractAddress.rawValue,
                            data = fetchResponse.disperseTx!!.data,
                            value = BigInteger.ZERO,
                            blockConfirmations = fetchResponse.disperseTx!!.blockConfirmations,
                            timestamp = fetchResponse.disperseTx!!.timestamp
                        ),
                        createdAt = fetchResponse.createdAt,
                        approveEvents = APPROVE_EVENTS.withDisperseContractAddress(disperseContractAddress),
                        disperseEvents = DISPERSE_EVENTS.withDisperseContractAddress(disperseContractAddress)
                    )
                )

            expectThat(fetchResponse.approveTx!!.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.disperseTx!!.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.approveTx!!.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.disperseTx!!.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("recipient has some balance of distributed token") {
            expectThat(tokenContract.balanceOf(recipientAddress.rawValue).send())
                .isEqualTo(amount.rawValue)
        }
    }

    @Test
    fun mustCorrectlyFetchAssetMultiSendRequestForSomeTokenWhenCustomRpcUrlIsSpecified() {
        val mainAccount = accounts[0]

        val tokenContract = suppose("simple asset contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(mainAccount.address),
                listOf(BigInteger("10000")),
                mainAccount.address
            ).send()
        }

        val disperseContract = suppose("simple disperse contract is deployed") {
            SimpleDisperse.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider()
            ).send()
        }

        val tokenAddress = ContractAddress(tokenContract.contractAddress)
        val disperseContractAddress = ContractAddress(disperseContract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val (projectId, chainId, apiKey) = suppose("project with customRpcUrl is inserted into database") {
            insertProjectWithCustomRpcUrl()
        }

        val createResponse = suppose("request to create asset multi-send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/multi-send")
                    .header(CustomHeaders.API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "disperse_contract_address": "${disperseContractAddress.rawValue}",
                                "asset_type": "TOKEN",
                                "items": [
                                    {
                                        "wallet_address": "${recipientAddress.rawValue}",
                                        "amount": "${amount.rawValue}",
                                        "item_name": "Example"
                                    }
                                ],
                                "sender_address": "${senderAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "approve_screen_config": {
                                    "before_action_message": "approve-before-action-message",
                                    "after_action_message": "approve-after-action-message"
                                },
                                "disperse_screen_config": {
                                    "before_action_message": "disperse-before-action-message",
                                    "after_action_message": "disperse-after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(createResponse.response.contentAsString, AssetMultiSendRequestResponse::class.java)
        }

        val approveTxHash = suppose("some asset approve transaction is made") {
            tokenContract.approve(disperseContractAddress.rawValue, amount.rawValue).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("approve transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("approve transaction info is attached to asset multi-send request") {
            assetMultiSendRequestRepository.setApproveTxInfo(createResponse.id, approveTxHash, senderAddress)
        }

        val disperseTxHash = suppose("some disperse transaction is made") {
            disperseContract.disperseToken(
                tokenAddress.rawValue,
                listOf(recipientAddress.rawValue),
                listOf(amount.rawValue)
            ).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("disperse transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("disperse transaction info is attached to asset multi-send request") {
            assetMultiSendRequestRepository.setDisperseTxInfo(createResponse.id, disperseTxHash, senderAddress)
        }

        val fetchResponse = suppose("request to fetch asset multi-send request is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/multi-send/${createResponse.id.value}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, AssetMultiSendRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    AssetMultiSendRequestResponse(
                        id = fetchResponse.id,
                        projectId = projectId,
                        approveStatus = Status.SUCCESS,
                        disperseStatus = Status.SUCCESS,
                        chainId = chainId.value,
                        tokenAddress = tokenAddress.rawValue,
                        disperseContractAddress = disperseContractAddress.rawValue,
                        assetType = AssetType.TOKEN,
                        items = listOf(
                            MultiSendItemResponse(
                                walletAddress = recipientAddress.rawValue,
                                amount = amount.rawValue,
                                itemName = "Example"
                            )
                        ),
                        senderAddress = senderAddress.rawValue,
                        arbitraryData = fetchResponse.arbitraryData,
                        approveScreenConfig = ScreenConfig(
                            beforeActionMessage = "approve-before-action-message",
                            afterActionMessage = "approve-after-action-message"
                        ),
                        disperseScreenConfig = ScreenConfig(
                            beforeActionMessage = "disperse-before-action-message",
                            afterActionMessage = "disperse-after-action-message"
                        ),
                        redirectUrl = PROJECT.baseRedirectUrl.value +
                            "/request-multi-send/${fetchResponse.id.value}/action",
                        approveTx = TransactionResponse(
                            txHash = approveTxHash.value,
                            from = senderAddress.rawValue,
                            to = tokenAddress.rawValue,
                            data = fetchResponse.approveTx!!.data,
                            value = BigInteger.ZERO,
                            blockConfirmations = fetchResponse.approveTx!!.blockConfirmations,
                            timestamp = fetchResponse.approveTx!!.timestamp
                        ),
                        disperseTx = TransactionResponse(
                            txHash = disperseTxHash.value,
                            from = senderAddress.rawValue,
                            to = disperseContractAddress.rawValue,
                            data = fetchResponse.disperseTx!!.data,
                            value = BigInteger.ZERO,
                            blockConfirmations = fetchResponse.disperseTx!!.blockConfirmations,
                            timestamp = fetchResponse.disperseTx!!.timestamp
                        ),
                        createdAt = fetchResponse.createdAt,
                        approveEvents = APPROVE_EVENTS.withDisperseContractAddress(disperseContractAddress),
                        disperseEvents = DISPERSE_EVENTS.withDisperseContractAddress(disperseContractAddress)
                    )
                )

            expectThat(fetchResponse.approveTx!!.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.disperseTx!!.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.approveTx!!.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.disperseTx!!.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("recipient has some balance of distributed token") {
            expectThat(tokenContract.balanceOf(recipientAddress.rawValue).send())
                .isEqualTo(amount.rawValue)
        }
    }

    @Test
    fun mustCorrectlyFetchAssetMultiSendRequestForNativeAsset() {
        val mainAccount = accounts[0]

        val disperseContract = suppose("simple disperse contract is deployed") {
            SimpleDisperse.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider()
            ).send()
        }

        val disperseContractAddress = ContractAddress(disperseContract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val initialEthBalance = hardhatContainer.web3j.ethGetBalance(
            recipientAddress.rawValue,
            DefaultBlockParameterName.LATEST
        ).send().balance

        val createResponse = suppose("request to create asset multi-send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/multi-send")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "disperse_contract_address": "${disperseContractAddress.rawValue}",
                                "asset_type": "NATIVE",
                                "items": [
                                    {
                                        "wallet_address": "${recipientAddress.rawValue}",
                                        "amount": "${amount.rawValue}",
                                        "item_name": "Example"
                                    }
                                ],
                                "sender_address": "${senderAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "approve_screen_config": {
                                    "before_action_message": "approve-before-action-message",
                                    "after_action_message": "approve-after-action-message"
                                },
                                "disperse_screen_config": {
                                    "before_action_message": "disperse-before-action-message",
                                    "after_action_message": "disperse-after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(createResponse.response.contentAsString, AssetMultiSendRequestResponse::class.java)
        }

        val disperseTxHash = suppose("some disperse transaction is made") {
            disperseContract.disperseEther(
                listOf(recipientAddress.rawValue),
                listOf(amount.rawValue),
                amount.rawValue
            ).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("disperse transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("disperse transaction info is attached to asset multi-send request") {
            assetMultiSendRequestRepository.setDisperseTxInfo(createResponse.id, disperseTxHash, senderAddress)
        }

        val fetchResponse = suppose("request to fetch asset multi-send request is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/multi-send/${createResponse.id.value}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, AssetMultiSendRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    AssetMultiSendRequestResponse(
                        id = fetchResponse.id,
                        projectId = PROJECT_ID,
                        approveStatus = null,
                        disperseStatus = Status.SUCCESS,
                        chainId = PROJECT.chainId.value,
                        tokenAddress = null,
                        disperseContractAddress = disperseContractAddress.rawValue,
                        assetType = AssetType.NATIVE,
                        items = listOf(
                            MultiSendItemResponse(
                                walletAddress = recipientAddress.rawValue,
                                amount = amount.rawValue,
                                itemName = "Example"
                            )
                        ),
                        senderAddress = senderAddress.rawValue,
                        arbitraryData = fetchResponse.arbitraryData,
                        approveScreenConfig = ScreenConfig(
                            beforeActionMessage = "approve-before-action-message",
                            afterActionMessage = "approve-after-action-message"
                        ),
                        disperseScreenConfig = ScreenConfig(
                            beforeActionMessage = "disperse-before-action-message",
                            afterActionMessage = "disperse-after-action-message"
                        ),
                        redirectUrl = PROJECT.baseRedirectUrl.value +
                            "/request-multi-send/${fetchResponse.id.value}/action",
                        approveTx = null,
                        disperseTx = TransactionResponse(
                            txHash = disperseTxHash.value,
                            from = senderAddress.rawValue,
                            to = disperseContractAddress.rawValue,
                            data = fetchResponse.disperseTx!!.data,
                            value = amount.rawValue,
                            blockConfirmations = fetchResponse.disperseTx!!.blockConfirmations,
                            timestamp = fetchResponse.disperseTx!!.timestamp
                        ),
                        createdAt = fetchResponse.createdAt,
                        approveEvents = null,
                        disperseEvents = emptyList()
                    )
                )

            expectThat(fetchResponse.disperseTx!!.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.disperseTx!!.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("recipient has some balance of distributed native asset") {
            val balance = hardhatContainer.web3j.ethGetBalance(
                recipientAddress.rawValue,
                DefaultBlockParameterName.LATEST
            ).send().balance

            expectThat(balance)
                .isEqualTo(initialEthBalance + amount.rawValue)
        }
    }

    @Test
    fun mustCorrectlyFetchAssetMultiSendRequestForNativeAssetWhenCustomRpcUrlIsSpecified() {
        val mainAccount = accounts[0]

        val disperseContract = suppose("simple disperse contract is deployed") {
            SimpleDisperse.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider()
            ).send()
        }

        val disperseContractAddress = ContractAddress(disperseContract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val initialEthBalance = hardhatContainer.web3j.ethGetBalance(
            recipientAddress.rawValue,
            DefaultBlockParameterName.LATEST
        ).send().balance

        val (projectId, chainId, apiKey) = suppose("project with customRpcUrl is inserted into database") {
            insertProjectWithCustomRpcUrl()
        }

        val createResponse = suppose("request to create asset multi-send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/multi-send")
                    .header(CustomHeaders.API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "disperse_contract_address": "${disperseContractAddress.rawValue}",
                                "asset_type": "NATIVE",
                                "items": [
                                    {
                                        "wallet_address": "${recipientAddress.rawValue}",
                                        "amount": "${amount.rawValue}",
                                        "item_name": "Example"
                                    }
                                ],
                                "sender_address": "${senderAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "approve_screen_config": {
                                    "before_action_message": "approve-before-action-message",
                                    "after_action_message": "approve-after-action-message"
                                },
                                "disperse_screen_config": {
                                    "before_action_message": "disperse-before-action-message",
                                    "after_action_message": "disperse-after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(createResponse.response.contentAsString, AssetMultiSendRequestResponse::class.java)
        }

        val disperseTxHash = suppose("some disperse transaction is made") {
            disperseContract.disperseEther(
                listOf(recipientAddress.rawValue),
                listOf(amount.rawValue),
                amount.rawValue
            ).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("disperse transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("disperse transaction info is attached to asset multi-send request") {
            assetMultiSendRequestRepository.setDisperseTxInfo(createResponse.id, disperseTxHash, senderAddress)
        }

        val fetchResponse = suppose("request to fetch asset multi-send request is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/multi-send/${createResponse.id.value}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, AssetMultiSendRequestResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    AssetMultiSendRequestResponse(
                        id = fetchResponse.id,
                        projectId = projectId,
                        approveStatus = null,
                        disperseStatus = Status.SUCCESS,
                        chainId = chainId.value,
                        tokenAddress = null,
                        disperseContractAddress = disperseContractAddress.rawValue,
                        assetType = AssetType.NATIVE,
                        items = listOf(
                            MultiSendItemResponse(
                                walletAddress = recipientAddress.rawValue,
                                amount = amount.rawValue,
                                itemName = "Example"
                            )
                        ),
                        senderAddress = senderAddress.rawValue,
                        arbitraryData = fetchResponse.arbitraryData,
                        approveScreenConfig = ScreenConfig(
                            beforeActionMessage = "approve-before-action-message",
                            afterActionMessage = "approve-after-action-message"
                        ),
                        disperseScreenConfig = ScreenConfig(
                            beforeActionMessage = "disperse-before-action-message",
                            afterActionMessage = "disperse-after-action-message"
                        ),
                        redirectUrl = PROJECT.baseRedirectUrl.value +
                            "/request-multi-send/${fetchResponse.id.value}/action",
                        approveTx = null,
                        disperseTx = TransactionResponse(
                            txHash = disperseTxHash.value,
                            from = senderAddress.rawValue,
                            to = disperseContractAddress.rawValue,
                            data = fetchResponse.disperseTx!!.data,
                            value = amount.rawValue,
                            blockConfirmations = fetchResponse.disperseTx!!.blockConfirmations,
                            timestamp = fetchResponse.disperseTx!!.timestamp
                        ),
                        createdAt = fetchResponse.createdAt,
                        approveEvents = null,
                        disperseEvents = emptyList()
                    )
                )

            expectThat(fetchResponse.disperseTx!!.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.disperseTx!!.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }

        verify("recipient has some balance of distributed native asset") {
            val balance = hardhatContainer.web3j.ethGetBalance(
                recipientAddress.rawValue,
                DefaultBlockParameterName.LATEST
            ).send().balance

            expectThat(balance)
                .isEqualTo(initialEthBalance + amount.rawValue)
        }
    }

    @Test
    fun mustReturn404NotFoundForNonExistentAssetMultiSendRequest() {
        verify("404 is returned for non-existent asset multi-send request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/multi-send/${UUID.randomUUID()}")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustCorrectlyFetchAssetMultiSendRequestsByProjectId() {
        val mainAccount = accounts[0]

        val tokenContract = suppose("simple asset contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(mainAccount.address),
                listOf(BigInteger("10000")),
                mainAccount.address
            ).send()
        }

        val disperseContract = suppose("simple disperse contract is deployed") {
            SimpleDisperse.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider()
            ).send()
        }

        val tokenAddress = ContractAddress(tokenContract.contractAddress)
        val disperseContractAddress = ContractAddress(disperseContract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val createResponse = suppose("request to create asset multi-send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/multi-send")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "disperse_contract_address": "${disperseContractAddress.rawValue}",
                                "asset_type": "TOKEN",
                                "items": [
                                    {
                                        "wallet_address": "${recipientAddress.rawValue}",
                                        "amount": "${amount.rawValue}",
                                        "item_name": "Example"
                                    }
                                ],
                                "sender_address": "${senderAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "approve_screen_config": {
                                    "before_action_message": "approve-before-action-message",
                                    "after_action_message": "approve-after-action-message"
                                },
                                "disperse_screen_config": {
                                    "before_action_message": "disperse-before-action-message",
                                    "after_action_message": "disperse-after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(createResponse.response.contentAsString, AssetMultiSendRequestResponse::class.java)
        }

        val approveTxHash = suppose("some asset approve transaction is made") {
            tokenContract.approve(disperseContractAddress.rawValue, amount.rawValue).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("approve transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("approve transaction info is attached to asset multi-send request") {
            assetMultiSendRequestRepository.setApproveTxInfo(createResponse.id, approveTxHash, senderAddress)
        }

        val disperseTxHash = suppose("some disperse transaction is made") {
            disperseContract.disperseToken(
                tokenAddress.rawValue,
                listOf(recipientAddress.rawValue),
                listOf(amount.rawValue)
            ).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("disperse transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("disperse transaction info is attached to asset multi-send request") {
            assetMultiSendRequestRepository.setDisperseTxInfo(createResponse.id, disperseTxHash, senderAddress)
        }

        val fetchResponse = suppose("request to fetch asset multi-send requests is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/multi-send/by-project/${createResponse.projectId.value}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, AssetMultiSendRequestsResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    AssetMultiSendRequestsResponse(
                        listOf(
                            AssetMultiSendRequestResponse(
                                id = fetchResponse.requests[0].id,
                                projectId = PROJECT_ID,
                                approveStatus = Status.SUCCESS,
                                disperseStatus = Status.SUCCESS,
                                chainId = PROJECT.chainId.value,
                                tokenAddress = tokenAddress.rawValue,
                                disperseContractAddress = disperseContractAddress.rawValue,
                                assetType = AssetType.TOKEN,
                                items = listOf(
                                    MultiSendItemResponse(
                                        walletAddress = recipientAddress.rawValue,
                                        amount = amount.rawValue,
                                        itemName = "Example"
                                    )
                                ),
                                senderAddress = senderAddress.rawValue,
                                arbitraryData = fetchResponse.requests[0].arbitraryData,
                                approveScreenConfig = ScreenConfig(
                                    beforeActionMessage = "approve-before-action-message",
                                    afterActionMessage = "approve-after-action-message"
                                ),
                                disperseScreenConfig = ScreenConfig(
                                    beforeActionMessage = "disperse-before-action-message",
                                    afterActionMessage = "disperse-after-action-message"
                                ),
                                redirectUrl = PROJECT.baseRedirectUrl.value +
                                    "/request-multi-send/${fetchResponse.requests[0].id.value}/action",
                                approveTx = TransactionResponse(
                                    txHash = approveTxHash.value,
                                    from = senderAddress.rawValue,
                                    to = tokenAddress.rawValue,
                                    data = fetchResponse.requests[0].approveTx!!.data,
                                    value = BigInteger.ZERO,
                                    blockConfirmations = fetchResponse.requests[0].approveTx!!.blockConfirmations,
                                    timestamp = fetchResponse.requests[0].approveTx!!.timestamp
                                ),
                                disperseTx = TransactionResponse(
                                    txHash = disperseTxHash.value,
                                    from = senderAddress.rawValue,
                                    to = disperseContractAddress.rawValue,
                                    data = fetchResponse.requests[0].disperseTx!!.data,
                                    value = BigInteger.ZERO,
                                    blockConfirmations = fetchResponse.requests[0].disperseTx!!.blockConfirmations,
                                    timestamp = fetchResponse.requests[0].disperseTx!!.timestamp
                                ),
                                createdAt = fetchResponse.requests[0].createdAt,
                                approveEvents = APPROVE_EVENTS.withDisperseContractAddress(disperseContractAddress),
                                disperseEvents = DISPERSE_EVENTS.withDisperseContractAddress(disperseContractAddress)
                            )
                        )
                    )
                )

            expectThat(fetchResponse.requests[0].approveTx!!.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.requests[0].disperseTx!!.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.requests[0].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.requests[0].approveTx!!.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.requests[0].disperseTx!!.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchAssetMultiSendRequestsByProjectIdWhenCustomRpcUrlIsSpecified() {
        val mainAccount = accounts[0]

        val tokenContract = suppose("simple asset contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(mainAccount.address),
                listOf(BigInteger("10000")),
                mainAccount.address
            ).send()
        }

        val disperseContract = suppose("simple disperse contract is deployed") {
            SimpleDisperse.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider()
            ).send()
        }

        val tokenAddress = ContractAddress(tokenContract.contractAddress)
        val disperseContractAddress = ContractAddress(disperseContract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val (projectId, chainId, apiKey) = suppose("project with customRpcUrl is inserted into database") {
            insertProjectWithCustomRpcUrl()
        }

        val createResponse = suppose("request to create asset multi-send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/multi-send")
                    .header(CustomHeaders.API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "disperse_contract_address": "${disperseContractAddress.rawValue}",
                                "asset_type": "TOKEN",
                                "items": [
                                    {
                                        "wallet_address": "${recipientAddress.rawValue}",
                                        "amount": "${amount.rawValue}",
                                        "item_name": "Example"
                                    }
                                ],
                                "sender_address": "${senderAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "approve_screen_config": {
                                    "before_action_message": "approve-before-action-message",
                                    "after_action_message": "approve-after-action-message"
                                },
                                "disperse_screen_config": {
                                    "before_action_message": "disperse-before-action-message",
                                    "after_action_message": "disperse-after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(createResponse.response.contentAsString, AssetMultiSendRequestResponse::class.java)
        }

        val approveTxHash = suppose("some asset approve transaction is made") {
            tokenContract.approve(disperseContractAddress.rawValue, amount.rawValue).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("approve transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("approve transaction info is attached to asset multi-send request") {
            assetMultiSendRequestRepository.setApproveTxInfo(createResponse.id, approveTxHash, senderAddress)
        }

        val disperseTxHash = suppose("some disperse transaction is made") {
            disperseContract.disperseToken(
                tokenAddress.rawValue,
                listOf(recipientAddress.rawValue),
                listOf(amount.rawValue)
            ).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("disperse transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("disperse transaction info is attached to asset multi-send request") {
            assetMultiSendRequestRepository.setDisperseTxInfo(createResponse.id, disperseTxHash, senderAddress)
        }

        val fetchResponse = suppose("request to fetch asset multi-send requests is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/multi-send/by-project/${createResponse.projectId.value}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, AssetMultiSendRequestsResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    AssetMultiSendRequestsResponse(
                        listOf(
                            AssetMultiSendRequestResponse(
                                id = fetchResponse.requests[0].id,
                                projectId = projectId,
                                approveStatus = Status.SUCCESS,
                                disperseStatus = Status.SUCCESS,
                                chainId = chainId.value,
                                tokenAddress = tokenAddress.rawValue,
                                disperseContractAddress = disperseContractAddress.rawValue,
                                assetType = AssetType.TOKEN,
                                items = listOf(
                                    MultiSendItemResponse(
                                        walletAddress = recipientAddress.rawValue,
                                        amount = amount.rawValue,
                                        itemName = "Example"
                                    )
                                ),
                                senderAddress = senderAddress.rawValue,
                                arbitraryData = fetchResponse.requests[0].arbitraryData,
                                approveScreenConfig = ScreenConfig(
                                    beforeActionMessage = "approve-before-action-message",
                                    afterActionMessage = "approve-after-action-message"
                                ),
                                disperseScreenConfig = ScreenConfig(
                                    beforeActionMessage = "disperse-before-action-message",
                                    afterActionMessage = "disperse-after-action-message"
                                ),
                                redirectUrl = PROJECT.baseRedirectUrl.value +
                                    "/request-multi-send/${fetchResponse.requests[0].id.value}/action",
                                approveTx = TransactionResponse(
                                    txHash = approveTxHash.value,
                                    from = senderAddress.rawValue,
                                    to = tokenAddress.rawValue,
                                    data = fetchResponse.requests[0].approveTx!!.data,
                                    value = BigInteger.ZERO,
                                    blockConfirmations = fetchResponse.requests[0].approveTx!!.blockConfirmations,
                                    timestamp = fetchResponse.requests[0].approveTx!!.timestamp
                                ),
                                disperseTx = TransactionResponse(
                                    txHash = disperseTxHash.value,
                                    from = senderAddress.rawValue,
                                    to = disperseContractAddress.rawValue,
                                    data = fetchResponse.requests[0].disperseTx!!.data,
                                    value = BigInteger.ZERO,
                                    blockConfirmations = fetchResponse.requests[0].disperseTx!!.blockConfirmations,
                                    timestamp = fetchResponse.requests[0].disperseTx!!.timestamp
                                ),
                                createdAt = fetchResponse.requests[0].createdAt,
                                approveEvents = APPROVE_EVENTS.withDisperseContractAddress(disperseContractAddress),
                                disperseEvents = DISPERSE_EVENTS.withDisperseContractAddress(disperseContractAddress)
                            )
                        )
                    )
                )

            expectThat(fetchResponse.requests[0].approveTx!!.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.requests[0].disperseTx!!.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.requests[0].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.requests[0].approveTx!!.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.requests[0].disperseTx!!.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchAssetMultiSendRequestsBySenderAddress() {
        val mainAccount = accounts[0]

        val tokenContract = suppose("simple asset contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(mainAccount.address),
                listOf(BigInteger("10000")),
                mainAccount.address
            ).send()
        }

        val disperseContract = suppose("simple disperse contract is deployed") {
            SimpleDisperse.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider()
            ).send()
        }

        val tokenAddress = ContractAddress(tokenContract.contractAddress)
        val disperseContractAddress = ContractAddress(disperseContract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val createResponse = suppose("request to create asset multi-send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/multi-send")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "disperse_contract_address": "${disperseContractAddress.rawValue}",
                                "asset_type": "TOKEN",
                                "items": [
                                    {
                                        "wallet_address": "${recipientAddress.rawValue}",
                                        "amount": "${amount.rawValue}",
                                        "item_name": "Example"
                                    }
                                ],
                                "sender_address": "${senderAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "approve_screen_config": {
                                    "before_action_message": "approve-before-action-message",
                                    "after_action_message": "approve-after-action-message"
                                },
                                "disperse_screen_config": {
                                    "before_action_message": "disperse-before-action-message",
                                    "after_action_message": "disperse-after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(createResponse.response.contentAsString, AssetMultiSendRequestResponse::class.java)
        }

        val approveTxHash = suppose("some asset approve transaction is made") {
            tokenContract.approve(disperseContractAddress.rawValue, amount.rawValue).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("approve transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("approve transaction info is attached to asset multi-send request") {
            assetMultiSendRequestRepository.setApproveTxInfo(createResponse.id, approveTxHash, senderAddress)
        }

        val disperseTxHash = suppose("some disperse transaction is made") {
            disperseContract.disperseToken(
                tokenAddress.rawValue,
                listOf(recipientAddress.rawValue),
                listOf(amount.rawValue)
            ).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("disperse transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("disperse transaction info is attached to asset multi-send request") {
            assetMultiSendRequestRepository.setDisperseTxInfo(createResponse.id, disperseTxHash, senderAddress)
        }

        val fetchResponse = suppose("request to fetch asset multi-send requests is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/multi-send/by-sender/${WalletAddress(mainAccount.address).rawValue}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, AssetMultiSendRequestsResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    AssetMultiSendRequestsResponse(
                        listOf(
                            AssetMultiSendRequestResponse(
                                id = fetchResponse.requests[0].id,
                                projectId = PROJECT_ID,
                                approveStatus = Status.SUCCESS,
                                disperseStatus = Status.SUCCESS,
                                chainId = PROJECT.chainId.value,
                                tokenAddress = tokenAddress.rawValue,
                                disperseContractAddress = disperseContractAddress.rawValue,
                                assetType = AssetType.TOKEN,
                                items = listOf(
                                    MultiSendItemResponse(
                                        walletAddress = recipientAddress.rawValue,
                                        amount = amount.rawValue,
                                        itemName = "Example"
                                    )
                                ),
                                senderAddress = senderAddress.rawValue,
                                arbitraryData = fetchResponse.requests[0].arbitraryData,
                                approveScreenConfig = ScreenConfig(
                                    beforeActionMessage = "approve-before-action-message",
                                    afterActionMessage = "approve-after-action-message"
                                ),
                                disperseScreenConfig = ScreenConfig(
                                    beforeActionMessage = "disperse-before-action-message",
                                    afterActionMessage = "disperse-after-action-message"
                                ),
                                redirectUrl = PROJECT.baseRedirectUrl.value +
                                    "/request-multi-send/${fetchResponse.requests[0].id.value}/action",
                                approveTx = TransactionResponse(
                                    txHash = approveTxHash.value,
                                    from = senderAddress.rawValue,
                                    to = tokenAddress.rawValue,
                                    data = fetchResponse.requests[0].approveTx!!.data,
                                    value = BigInteger.ZERO,
                                    blockConfirmations = fetchResponse.requests[0].approveTx!!.blockConfirmations,
                                    timestamp = fetchResponse.requests[0].approveTx!!.timestamp
                                ),
                                disperseTx = TransactionResponse(
                                    txHash = disperseTxHash.value,
                                    from = senderAddress.rawValue,
                                    to = disperseContractAddress.rawValue,
                                    data = fetchResponse.requests[0].disperseTx!!.data,
                                    value = BigInteger.ZERO,
                                    blockConfirmations = fetchResponse.requests[0].disperseTx!!.blockConfirmations,
                                    timestamp = fetchResponse.requests[0].disperseTx!!.timestamp
                                ),
                                createdAt = fetchResponse.requests[0].createdAt,
                                approveEvents = APPROVE_EVENTS.withDisperseContractAddress(disperseContractAddress),
                                disperseEvents = DISPERSE_EVENTS.withDisperseContractAddress(disperseContractAddress)
                            )
                        )
                    )
                )

            expectThat(fetchResponse.requests[0].approveTx!!.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.requests[0].disperseTx!!.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.requests[0].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.requests[0].approveTx!!.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.requests[0].disperseTx!!.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyFetchAssetMultiSendRequestsBySenderAddressWhenCustomRpcUrlIsSpecified() {
        val mainAccount = accounts[0]

        val tokenContract = suppose("simple asset contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(mainAccount.address),
                listOf(BigInteger("10000")),
                mainAccount.address
            ).send()
        }

        val disperseContract = suppose("simple disperse contract is deployed") {
            SimpleDisperse.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider()
            ).send()
        }

        val tokenAddress = ContractAddress(tokenContract.contractAddress)
        val disperseContractAddress = ContractAddress(disperseContract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val (projectId, chainId, apiKey) = suppose("project with customRpcUrl is inserted into database") {
            insertProjectWithCustomRpcUrl()
        }

        val createResponse = suppose("request to create asset multi-send request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/multi-send")
                    .header(CustomHeaders.API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "token_address": "${tokenAddress.rawValue}",
                                "disperse_contract_address": "${disperseContractAddress.rawValue}",
                                "asset_type": "TOKEN",
                                "items": [
                                    {
                                        "wallet_address": "${recipientAddress.rawValue}",
                                        "amount": "${amount.rawValue}",
                                        "item_name": "Example"
                                    }
                                ],
                                "sender_address": "${senderAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "approve_screen_config": {
                                    "before_action_message": "approve-before-action-message",
                                    "after_action_message": "approve-after-action-message"
                                },
                                "disperse_screen_config": {
                                    "before_action_message": "disperse-before-action-message",
                                    "after_action_message": "disperse-after-action-message"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(createResponse.response.contentAsString, AssetMultiSendRequestResponse::class.java)
        }

        val approveTxHash = suppose("some asset approve transaction is made") {
            tokenContract.approve(disperseContractAddress.rawValue, amount.rawValue).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("approve transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("approve transaction info is attached to asset multi-send request") {
            assetMultiSendRequestRepository.setApproveTxInfo(createResponse.id, approveTxHash, senderAddress)
        }

        val disperseTxHash = suppose("some disperse transaction is made") {
            disperseContract.disperseToken(
                tokenAddress.rawValue,
                listOf(recipientAddress.rawValue),
                listOf(amount.rawValue)
            ).send()
                ?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("disperse transaction will have at least one block confirmation") {
            hardhatContainer.mine()
        }

        suppose("disperse transaction info is attached to asset multi-send request") {
            assetMultiSendRequestRepository.setDisperseTxInfo(createResponse.id, disperseTxHash, senderAddress)
        }

        val fetchResponse = suppose("request to fetch asset multi-send requests is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/multi-send/by-sender/${WalletAddress(mainAccount.address).rawValue}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, AssetMultiSendRequestsResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(fetchResponse)
                .isEqualTo(
                    AssetMultiSendRequestsResponse(
                        listOf(
                            AssetMultiSendRequestResponse(
                                id = fetchResponse.requests[0].id,
                                projectId = projectId,
                                approveStatus = Status.SUCCESS,
                                disperseStatus = Status.SUCCESS,
                                chainId = chainId.value,
                                tokenAddress = tokenAddress.rawValue,
                                disperseContractAddress = disperseContractAddress.rawValue,
                                assetType = AssetType.TOKEN,
                                items = listOf(
                                    MultiSendItemResponse(
                                        walletAddress = recipientAddress.rawValue,
                                        amount = amount.rawValue,
                                        itemName = "Example"
                                    )
                                ),
                                senderAddress = senderAddress.rawValue,
                                arbitraryData = fetchResponse.requests[0].arbitraryData,
                                approveScreenConfig = ScreenConfig(
                                    beforeActionMessage = "approve-before-action-message",
                                    afterActionMessage = "approve-after-action-message"
                                ),
                                disperseScreenConfig = ScreenConfig(
                                    beforeActionMessage = "disperse-before-action-message",
                                    afterActionMessage = "disperse-after-action-message"
                                ),
                                redirectUrl = PROJECT.baseRedirectUrl.value +
                                    "/request-multi-send/${fetchResponse.requests[0].id.value}/action",
                                approveTx = TransactionResponse(
                                    txHash = approveTxHash.value,
                                    from = senderAddress.rawValue,
                                    to = tokenAddress.rawValue,
                                    data = fetchResponse.requests[0].approveTx!!.data,
                                    value = BigInteger.ZERO,
                                    blockConfirmations = fetchResponse.requests[0].approveTx!!.blockConfirmations,
                                    timestamp = fetchResponse.requests[0].approveTx!!.timestamp
                                ),
                                disperseTx = TransactionResponse(
                                    txHash = disperseTxHash.value,
                                    from = senderAddress.rawValue,
                                    to = disperseContractAddress.rawValue,
                                    data = fetchResponse.requests[0].disperseTx!!.data,
                                    value = BigInteger.ZERO,
                                    blockConfirmations = fetchResponse.requests[0].disperseTx!!.blockConfirmations,
                                    timestamp = fetchResponse.requests[0].disperseTx!!.timestamp
                                ),
                                createdAt = fetchResponse.requests[0].createdAt,
                                approveEvents = APPROVE_EVENTS.withDisperseContractAddress(disperseContractAddress),
                                disperseEvents = DISPERSE_EVENTS.withDisperseContractAddress(disperseContractAddress)
                            )
                        )
                    )
                )

            expectThat(fetchResponse.requests[0].approveTx!!.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.requests[0].disperseTx!!.blockConfirmations)
                .isNotZero()
            expectThat(fetchResponse.requests[0].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.requests[0].approveTx!!.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(fetchResponse.requests[0].disperseTx!!.timestamp)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    fun mustCorrectlyAttachApproveTransactionInfo() {
        val id = AssetMultiSendRequestId(UUID.randomUUID())
        val tokenSender = WalletAddress("d")

        suppose("some asset multi-send request without approve transaction info exists in database") {
            assetMultiSendRequestRepository.store(
                StoreAssetMultiSendRequestParams(
                    id = id,
                    projectId = PROJECT_ID,
                    chainId = TestData.CHAIN_ID,
                    redirectUrl = "https://example.com/${id.value}",
                    tokenAddress = ContractAddress("a"),
                    disperseContractAddress = ContractAddress("b"),
                    assetAmounts = listOf(Balance(BigInteger.TEN)),
                    assetRecipientAddresses = listOf(WalletAddress("c")),
                    itemNames = listOf("Example"),
                    assetSenderAddress = tokenSender,
                    arbitraryData = TestData.EMPTY_JSON_OBJECT,
                    approveScreenConfig = ScreenConfig(
                        beforeActionMessage = "approve-before-action-message",
                        afterActionMessage = "approve-after-action-message"
                    ),
                    disperseScreenConfig = ScreenConfig(
                        beforeActionMessage = "disperse-before-action-message",
                        afterActionMessage = "disperse-after-action-message"
                    ),
                    createdAt = TestData.TIMESTAMP
                )
            )
        }

        val approveTxHash = TransactionHash("0x1")

        suppose("request to attach approve transaction info to asset multi-send request is made") {
            mockMvc.perform(
                MockMvcRequestBuilders.put("/v1/multi-send/${id.value}/approve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "tx_hash": "${approveTxHash.value}",
                                "caller_address": "${tokenSender.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        }

        verify("approve transaction info is correctly attached to asset multi-send request") {
            val storedRequest = assetMultiSendRequestRepository.getById(id)

            expectThat(storedRequest?.approveTxHash)
                .isEqualTo(approveTxHash)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenApproveTransactionInfoIsNotAttached() {
        val id = AssetMultiSendRequestId(UUID.randomUUID())
        val approveTxHash = TransactionHash("0x1")
        val tokenSender = WalletAddress("b")

        suppose("some asset multi-send request with approve transaction info exists in database") {
            assetMultiSendRequestRepository.store(
                StoreAssetMultiSendRequestParams(
                    id = id,
                    projectId = PROJECT_ID,
                    chainId = TestData.CHAIN_ID,
                    redirectUrl = "https://example.com/${id.value}",
                    tokenAddress = ContractAddress("a"),
                    disperseContractAddress = ContractAddress("b"),
                    assetAmounts = listOf(Balance(BigInteger.TEN)),
                    assetRecipientAddresses = listOf(WalletAddress("c")),
                    itemNames = listOf("Example"),
                    assetSenderAddress = tokenSender,
                    arbitraryData = TestData.EMPTY_JSON_OBJECT,
                    approveScreenConfig = ScreenConfig(
                        beforeActionMessage = "approve-before-action-message",
                        afterActionMessage = "approve-after-action-message"
                    ),
                    disperseScreenConfig = ScreenConfig(
                        beforeActionMessage = "disperse-before-action-message",
                        afterActionMessage = "disperse-after-action-message"
                    ),
                    createdAt = TestData.TIMESTAMP
                )
            )
            assetMultiSendRequestRepository.setApproveTxInfo(id, approveTxHash, tokenSender)
        }

        verify("400 is returned when attaching approve transaction info") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.put("/v1/multi-send/${id.value}/approve")
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

        verify("approve transaction info is not changed in database") {
            val storedRequest = assetMultiSendRequestRepository.getById(id)

            expectThat(storedRequest?.approveTxHash)
                .isEqualTo(approveTxHash)
        }
    }

    @Test
    fun mustCorrectlyAttachDisperseTransactionInfo() {
        val id = AssetMultiSendRequestId(UUID.randomUUID())
        val tokenSender = WalletAddress("d")

        suppose("some asset multi-send request without disperse transaction info exists in database") {
            assetMultiSendRequestRepository.store(
                StoreAssetMultiSendRequestParams(
                    id = id,
                    projectId = PROJECT_ID,
                    chainId = TestData.CHAIN_ID,
                    redirectUrl = "https://example.com/${id.value}",
                    tokenAddress = null,
                    disperseContractAddress = ContractAddress("b"),
                    assetAmounts = listOf(Balance(BigInteger.TEN)),
                    assetRecipientAddresses = listOf(WalletAddress("c")),
                    itemNames = listOf("Example"),
                    assetSenderAddress = tokenSender,
                    arbitraryData = TestData.EMPTY_JSON_OBJECT,
                    approveScreenConfig = ScreenConfig(
                        beforeActionMessage = "approve-before-action-message",
                        afterActionMessage = "approve-after-action-message"
                    ),
                    disperseScreenConfig = ScreenConfig(
                        beforeActionMessage = "disperse-before-action-message",
                        afterActionMessage = "disperse-after-action-message"
                    ),
                    createdAt = TestData.TIMESTAMP
                )
            )
        }

        val disperseTxHash = TransactionHash("0x1")

        suppose("request to attach disperse transaction info to asset multi-send request is made") {
            mockMvc.perform(
                MockMvcRequestBuilders.put("/v1/multi-send/${id.value}/disperse")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "tx_hash": "${disperseTxHash.value}",
                                "caller_address": "${tokenSender.rawValue}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        }

        verify("disperse transaction info is correctly attached to asset multi-send request") {
            val storedRequest = assetMultiSendRequestRepository.getById(id)

            expectThat(storedRequest?.disperseTxHash)
                .isEqualTo(disperseTxHash)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenDisperseTransactionInfoIsNotAttached() {
        val id = AssetMultiSendRequestId(UUID.randomUUID())
        val disperseTxHash = TransactionHash("0x1")
        val tokenSender = WalletAddress("b")

        suppose("some asset multi-send request with disperse transaction info exists in database") {
            assetMultiSendRequestRepository.store(
                StoreAssetMultiSendRequestParams(
                    id = id,
                    projectId = PROJECT_ID,
                    chainId = TestData.CHAIN_ID,
                    redirectUrl = "https://example.com/${id.value}",
                    tokenAddress = null,
                    disperseContractAddress = ContractAddress("b"),
                    assetAmounts = listOf(Balance(BigInteger.TEN)),
                    assetRecipientAddresses = listOf(WalletAddress("c")),
                    itemNames = listOf("Example"),
                    assetSenderAddress = tokenSender,
                    arbitraryData = TestData.EMPTY_JSON_OBJECT,
                    approveScreenConfig = ScreenConfig(
                        beforeActionMessage = "approve-before-action-message",
                        afterActionMessage = "approve-after-action-message"
                    ),
                    disperseScreenConfig = ScreenConfig(
                        beforeActionMessage = "disperse-before-action-message",
                        afterActionMessage = "disperse-after-action-message"
                    ),
                    createdAt = TestData.TIMESTAMP
                )
            )
            assetMultiSendRequestRepository.setDisperseTxInfo(id, disperseTxHash, tokenSender)
        }

        verify("400 is returned when attaching disperse transaction info") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.put("/v1/multi-send/${id.value}/disperse")
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

        verify("disperse transaction info is not changed in database") {
            val storedRequest = assetMultiSendRequestRepository.getById(id)

            expectThat(storedRequest?.disperseTxHash)
                .isEqualTo(disperseTxHash)
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

    private fun List<EventInfoResponse>.withDisperseContractAddress(disperseContractAddress: ContractAddress) =
        map { event ->
            event.copy(
                arguments = event.arguments.map { arg ->
                    arg.copy(
                        value = arg.value?.toString()
                            ?.replace("{disperseContractAddress}", disperseContractAddress.rawValue)
                    )
                }
            )
        }
}
