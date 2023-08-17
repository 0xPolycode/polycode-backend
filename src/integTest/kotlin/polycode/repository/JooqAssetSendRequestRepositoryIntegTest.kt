package polycode.repository

import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import polycode.TestBase
import polycode.TestData
import polycode.config.DatabaseConfig
import polycode.features.asset.send.model.params.StoreAssetSendRequestParams
import polycode.features.asset.send.model.result.AssetSendRequest
import polycode.features.asset.send.repository.JooqAssetSendRequestRepository
import polycode.generated.jooq.enums.UserIdentifierType
import polycode.generated.jooq.id.AssetSendRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import polycode.generated.jooq.tables.records.AssetSendRequestRecord
import polycode.generated.jooq.tables.records.ProjectRecord
import polycode.generated.jooq.tables.records.UserIdentifierRecord
import polycode.model.ScreenConfig
import polycode.testcontainers.SharedTestContainers
import polycode.util.Balance
import polycode.util.BaseUrl
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.TransactionHash
import polycode.util.WalletAddress
import java.math.BigInteger
import java.util.UUID

@JooqTest
@Import(JooqAssetSendRequestRepository::class, DatabaseConfig::class)
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqAssetSendRequestRepositoryIntegTest : TestBase() {

    companion object {
        private val CHAIN_ID = ChainId(1337L)
        private const val REDIRECT_URL = "redirect-url"
        private val TOKEN_ADDRESS = ContractAddress("a")
        private val ASSET_AMOUNT = Balance(BigInteger.valueOf(123456L))
        private val ASSET_SENDER_ADDRESS = WalletAddress("b")
        private val ASSET_RECIPIENT_ADDRESS = WalletAddress("c")
        private val ARBITRARY_DATA = TestData.EMPTY_JSON_OBJECT
        private const val SEND_SCREEN_BEFORE_ACTION_MESSAGE = "send-screen-before-action-message"
        private const val SEND_SCREEN_AFTER_ACTION_MESSAGE = "send-screen-after-action-message"
        private val TX_HASH = TransactionHash("tx-hash")
        private val PROJECT_ID = ProjectId(UUID.randomUUID())
        private val OWNER_ID = UserId(UUID.randomUUID())
    }

    @Suppress("unused")
    private val postgresContainer = SharedTestContainers.postgresContainer

    @Autowired
    private lateinit var repository: JooqAssetSendRequestRepository

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
                id = PROJECT_ID,
                ownerId = OWNER_ID,
                baseRedirectUrl = BaseUrl("base-redirect-url"),
                chainId = ChainId(1337L),
                customRpcUrl = "custom-rpc-url",
                createdAt = TestData.TIMESTAMP
            )
        )
    }

    @Test
    fun mustCorrectlyFetchAssetSendRequestById() {
        val id = AssetSendRequestId(UUID.randomUUID())

        suppose("some asset send request exists in database") {
            dslContext.executeInsert(
                AssetSendRequestRecord(
                    id = id,
                    projectId = PROJECT_ID,
                    chainId = CHAIN_ID,
                    redirectUrl = REDIRECT_URL,
                    tokenAddress = TOKEN_ADDRESS,
                    assetAmount = ASSET_AMOUNT,
                    assetSenderAddress = ASSET_SENDER_ADDRESS,
                    assetRecipientAddress = ASSET_RECIPIENT_ADDRESS,
                    arbitraryData = ARBITRARY_DATA,
                    screenBeforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                    screenAfterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE,
                    txHash = TX_HASH,
                    createdAt = TestData.TIMESTAMP
                )
            )
        }

        verify("asset send request is correctly fetched by ID") {
            val result = repository.getById(id)

            expectThat(result)
                .isEqualTo(
                    AssetSendRequest(
                        id = id,
                        projectId = PROJECT_ID,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        tokenAddress = TOKEN_ADDRESS,
                        assetAmount = ASSET_AMOUNT,
                        assetSenderAddress = ASSET_SENDER_ADDRESS,
                        assetRecipientAddress = ASSET_RECIPIENT_ADDRESS,
                        txHash = TX_HASH,
                        arbitraryData = ARBITRARY_DATA,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentAssetSendRequestById() {
        verify("null is returned when fetching non-existent asset send request") {
            val result = repository.getById(AssetSendRequestId(UUID.randomUUID()))

            expectThat(result)
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyFetchAssetSendRequestsByProject() {
        val otherProjectId = ProjectId(UUID.randomUUID())

        suppose("some other project is in database") {
            dslContext.executeInsert(
                ProjectRecord(
                    id = otherProjectId,
                    ownerId = OWNER_ID,
                    baseRedirectUrl = BaseUrl("base-redirect-url"),
                    chainId = ChainId(1337L),
                    customRpcUrl = "custom-rpc-url",
                    createdAt = TestData.TIMESTAMP
                )
            )
        }

        val projectRequests = listOf(
            AssetSendRequestRecord(
                id = AssetSendRequestId(UUID.randomUUID()),
                projectId = PROJECT_ID,
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                assetAmount = ASSET_AMOUNT,
                assetSenderAddress = ASSET_SENDER_ADDRESS,
                assetRecipientAddress = ASSET_RECIPIENT_ADDRESS,
                arbitraryData = ARBITRARY_DATA,
                screenBeforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                screenAfterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE,
                txHash = TX_HASH,
                createdAt = TestData.TIMESTAMP
            ),
            AssetSendRequestRecord(
                id = AssetSendRequestId(UUID.randomUUID()),
                projectId = PROJECT_ID,
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                assetAmount = ASSET_AMOUNT,
                assetSenderAddress = ASSET_SENDER_ADDRESS,
                assetRecipientAddress = ASSET_RECIPIENT_ADDRESS,
                arbitraryData = ARBITRARY_DATA,
                screenBeforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                screenAfterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE,
                txHash = TX_HASH,
                createdAt = TestData.TIMESTAMP
            )
        )
        val otherRequests = listOf(
            AssetSendRequestRecord(
                id = AssetSendRequestId(UUID.randomUUID()),
                projectId = otherProjectId,
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                assetAmount = ASSET_AMOUNT,
                assetSenderAddress = WalletAddress("dead"),
                assetRecipientAddress = ASSET_RECIPIENT_ADDRESS,
                arbitraryData = ARBITRARY_DATA,
                screenBeforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                screenAfterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE,
                txHash = TX_HASH,
                createdAt = TestData.TIMESTAMP
            ),
            AssetSendRequestRecord(
                id = AssetSendRequestId(UUID.randomUUID()),
                projectId = otherProjectId,
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                assetAmount = ASSET_AMOUNT,
                assetSenderAddress = null,
                assetRecipientAddress = ASSET_RECIPIENT_ADDRESS,
                arbitraryData = ARBITRARY_DATA,
                screenBeforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                screenAfterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE,
                txHash = TX_HASH,
                createdAt = TestData.TIMESTAMP
            )
        )

        suppose("some asset send requests exist in database") {
            dslContext.batchInsert(projectRequests + otherRequests).execute()
        }

        verify("asset send requests are correctly fetched by project") {
            val result = repository.getAllByProjectId(PROJECT_ID)

            expectThat(result)
                .containsExactlyInAnyOrderElementsOf(
                    projectRequests.map {
                        AssetSendRequest(
                            id = it.id,
                            projectId = it.projectId,
                            chainId = it.chainId,
                            redirectUrl = it.redirectUrl,
                            tokenAddress = it.tokenAddress,
                            assetAmount = it.assetAmount,
                            assetSenderAddress = it.assetSenderAddress,
                            assetRecipientAddress = it.assetRecipientAddress,
                            txHash = it.txHash,
                            arbitraryData = it.arbitraryData,
                            screenConfig = ScreenConfig(
                                beforeActionMessage = it.screenBeforeActionMessage,
                                afterActionMessage = it.screenAfterActionMessage
                            ),
                            createdAt = it.createdAt
                        )
                    }
                )
        }
    }

    @Test
    fun mustCorrectlyFetchAssetSendRequestsBySender() {
        val senderRequests = listOf(
            AssetSendRequestRecord(
                id = AssetSendRequestId(UUID.randomUUID()),
                projectId = PROJECT_ID,
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                assetAmount = ASSET_AMOUNT,
                assetSenderAddress = ASSET_SENDER_ADDRESS,
                assetRecipientAddress = ASSET_RECIPIENT_ADDRESS,
                arbitraryData = ARBITRARY_DATA,
                screenBeforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                screenAfterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE,
                txHash = TX_HASH,
                createdAt = TestData.TIMESTAMP
            ),
            AssetSendRequestRecord(
                id = AssetSendRequestId(UUID.randomUUID()),
                projectId = PROJECT_ID,
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                assetAmount = ASSET_AMOUNT,
                assetSenderAddress = ASSET_SENDER_ADDRESS,
                assetRecipientAddress = ASSET_RECIPIENT_ADDRESS,
                arbitraryData = ARBITRARY_DATA,
                screenBeforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                screenAfterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE,
                txHash = TX_HASH,
                createdAt = TestData.TIMESTAMP
            )
        )
        val otherRequests = listOf(
            AssetSendRequestRecord(
                id = AssetSendRequestId(UUID.randomUUID()),
                projectId = PROJECT_ID,
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                assetAmount = ASSET_AMOUNT,
                assetSenderAddress = WalletAddress("dead"),
                assetRecipientAddress = ASSET_RECIPIENT_ADDRESS,
                arbitraryData = ARBITRARY_DATA,
                screenBeforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                screenAfterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE,
                txHash = TX_HASH,
                createdAt = TestData.TIMESTAMP
            ),
            AssetSendRequestRecord(
                id = AssetSendRequestId(UUID.randomUUID()),
                projectId = PROJECT_ID,
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                assetAmount = ASSET_AMOUNT,
                assetSenderAddress = null,
                assetRecipientAddress = ASSET_RECIPIENT_ADDRESS,
                arbitraryData = ARBITRARY_DATA,
                screenBeforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                screenAfterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE,
                txHash = TX_HASH,
                createdAt = TestData.TIMESTAMP
            )
        )

        suppose("some asset send requests exist in database") {
            dslContext.batchInsert(senderRequests + otherRequests).execute()
        }

        verify("asset send requests are correctly fetched by sender") {
            val result = repository.getBySender(ASSET_SENDER_ADDRESS)

            expectThat(result)
                .containsExactlyInAnyOrderElementsOf(
                    senderRequests.map {
                        AssetSendRequest(
                            id = it.id,
                            projectId = it.projectId,
                            chainId = it.chainId,
                            redirectUrl = it.redirectUrl,
                            tokenAddress = it.tokenAddress,
                            assetAmount = it.assetAmount,
                            assetSenderAddress = it.assetSenderAddress,
                            assetRecipientAddress = it.assetRecipientAddress,
                            txHash = it.txHash,
                            arbitraryData = it.arbitraryData,
                            screenConfig = ScreenConfig(
                                beforeActionMessage = it.screenBeforeActionMessage,
                                afterActionMessage = it.screenAfterActionMessage
                            ),
                            createdAt = it.createdAt
                        )
                    }
                )
        }
    }

    @Test
    fun mustCorrectlyFetchAssetSendRequestsByRecipient() {
        val recipientRequests = listOf(
            AssetSendRequestRecord(
                id = AssetSendRequestId(UUID.randomUUID()),
                projectId = PROJECT_ID,
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                assetAmount = ASSET_AMOUNT,
                assetSenderAddress = ASSET_SENDER_ADDRESS,
                assetRecipientAddress = ASSET_RECIPIENT_ADDRESS,
                arbitraryData = ARBITRARY_DATA,
                screenBeforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                screenAfterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE,
                txHash = TX_HASH,
                createdAt = TestData.TIMESTAMP
            ),
            AssetSendRequestRecord(
                id = AssetSendRequestId(UUID.randomUUID()),
                projectId = PROJECT_ID,
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                assetAmount = ASSET_AMOUNT,
                assetSenderAddress = ASSET_SENDER_ADDRESS,
                assetRecipientAddress = ASSET_RECIPIENT_ADDRESS,
                arbitraryData = ARBITRARY_DATA,
                screenBeforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                screenAfterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE,
                txHash = TX_HASH,
                createdAt = TestData.TIMESTAMP
            )
        )
        val otherRequests = listOf(
            AssetSendRequestRecord(
                id = AssetSendRequestId(UUID.randomUUID()),
                projectId = PROJECT_ID,
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                assetAmount = ASSET_AMOUNT,
                assetSenderAddress = ASSET_SENDER_ADDRESS,
                assetRecipientAddress = WalletAddress("dead"),
                arbitraryData = ARBITRARY_DATA,
                screenBeforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                screenAfterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE,
                txHash = TX_HASH,
                createdAt = TestData.TIMESTAMP
            )
        )

        suppose("some asset send requests exist in database") {
            dslContext.batchInsert(recipientRequests + otherRequests).execute()
        }

        verify("asset send requests are correctly fetched by recipient") {
            val result = repository.getByRecipient(ASSET_RECIPIENT_ADDRESS)

            expectThat(result)
                .containsExactlyInAnyOrderElementsOf(
                    recipientRequests.map {
                        AssetSendRequest(
                            id = it.id,
                            projectId = it.projectId,
                            chainId = it.chainId,
                            redirectUrl = it.redirectUrl,
                            tokenAddress = it.tokenAddress,
                            assetAmount = it.assetAmount,
                            assetSenderAddress = it.assetSenderAddress,
                            assetRecipientAddress = it.assetRecipientAddress,
                            txHash = it.txHash,
                            arbitraryData = it.arbitraryData,
                            screenConfig = ScreenConfig(
                                beforeActionMessage = it.screenBeforeActionMessage,
                                afterActionMessage = it.screenAfterActionMessage
                            ),
                            createdAt = it.createdAt
                        )
                    }
                )
        }
    }

    @Test
    fun mustCorrectlyStoreAssetSendRequest() {
        val id = AssetSendRequestId(UUID.randomUUID())
        val params = StoreAssetSendRequestParams(
            id = id,
            projectId = PROJECT_ID,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            assetAmount = ASSET_AMOUNT,
            assetSenderAddress = ASSET_SENDER_ADDRESS,
            assetRecipientAddress = ASSET_RECIPIENT_ADDRESS,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
            ),
            createdAt = TestData.TIMESTAMP
        )

        val storedAssetSendRequest = suppose("asset send request is stored in database") {
            repository.store(params)
        }

        val expectedAssetSendRequest = AssetSendRequest(
            id = id,
            projectId = PROJECT_ID,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            assetAmount = ASSET_AMOUNT,
            assetSenderAddress = ASSET_SENDER_ADDRESS,
            assetRecipientAddress = ASSET_RECIPIENT_ADDRESS,
            txHash = null,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
            ),
            createdAt = TestData.TIMESTAMP
        )

        verify("storing asset send request returns correct result") {
            expectThat(storedAssetSendRequest)
                .isEqualTo(expectedAssetSendRequest)
        }

        verify("asset send request was stored in database") {
            val result = repository.getById(id)

            expectThat(result)
                .isEqualTo(expectedAssetSendRequest)
        }
    }

    @Test
    fun mustCorrectlySetTxInfoForAssetSendRequestWithNullTxHash() {
        val id = AssetSendRequestId(UUID.randomUUID())
        val params = StoreAssetSendRequestParams(
            id = id,
            projectId = PROJECT_ID,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            assetAmount = ASSET_AMOUNT,
            assetSenderAddress = ASSET_SENDER_ADDRESS,
            assetRecipientAddress = ASSET_RECIPIENT_ADDRESS,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
            ),
            createdAt = TestData.TIMESTAMP
        )

        suppose("asset send request is stored in database") {
            repository.store(params)
        }

        verify("setting txInfo will succeed") {
            expectThat(repository.setTxInfo(id, TX_HASH, ASSET_SENDER_ADDRESS))
                .isTrue()
        }

        verify("txInfo was correctly set in database") {
            val result = repository.getById(id)

            expectThat(result)
                .isEqualTo(
                    AssetSendRequest(
                        id = id,
                        projectId = PROJECT_ID,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        tokenAddress = TOKEN_ADDRESS,
                        assetAmount = ASSET_AMOUNT,
                        assetSenderAddress = ASSET_SENDER_ADDRESS,
                        assetRecipientAddress = ASSET_RECIPIENT_ADDRESS,
                        txHash = TX_HASH,
                        arbitraryData = ARBITRARY_DATA,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustNotUpdateTokenSenderAddressForAssetSendRequestWhenTokenSenderIsAlreadySet() {
        val id = AssetSendRequestId(UUID.randomUUID())
        val params = StoreAssetSendRequestParams(
            id = id,
            projectId = PROJECT_ID,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            assetAmount = ASSET_AMOUNT,
            assetSenderAddress = ASSET_SENDER_ADDRESS,
            assetRecipientAddress = ASSET_RECIPIENT_ADDRESS,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
            ),
            createdAt = TestData.TIMESTAMP
        )

        suppose("asset send request is stored in database") {
            repository.store(params)
        }

        verify("setting txInfo will succeed") {
            val ignoredTokenSender = WalletAddress("f")
            expectThat(repository.setTxInfo(id, TX_HASH, ignoredTokenSender))
                .isTrue()
        }

        verify("txHash was correctly set while token sender was not updated") {
            val result = repository.getById(id)

            expectThat(result)
                .isEqualTo(
                    AssetSendRequest(
                        id = id,
                        chainId = CHAIN_ID,
                        projectId = PROJECT_ID,
                        redirectUrl = REDIRECT_URL,
                        tokenAddress = TOKEN_ADDRESS,
                        assetAmount = ASSET_AMOUNT,
                        assetSenderAddress = ASSET_SENDER_ADDRESS,
                        assetRecipientAddress = ASSET_RECIPIENT_ADDRESS,
                        txHash = TX_HASH,
                        arbitraryData = ARBITRARY_DATA,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustNotSetTxHashForAssetSendRequestWhenTxHashIsAlreadySet() {
        val id = AssetSendRequestId(UUID.randomUUID())
        val params = StoreAssetSendRequestParams(
            id = id,
            chainId = CHAIN_ID,
            projectId = PROJECT_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            assetAmount = ASSET_AMOUNT,
            assetSenderAddress = ASSET_SENDER_ADDRESS,
            assetRecipientAddress = ASSET_RECIPIENT_ADDRESS,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
            ),
            createdAt = TestData.TIMESTAMP
        )

        suppose("asset send request is stored in database") {
            repository.store(params)
        }

        verify("setting txInfo will succeed") {
            expectThat(repository.setTxInfo(id, TX_HASH, ASSET_SENDER_ADDRESS))
                .isTrue()
        }

        verify("setting another txInfo will not succeed") {
            expectThat(
                repository.setTxInfo(
                    id,
                    TransactionHash("different-tx-hash"),
                    ASSET_SENDER_ADDRESS
                )
            ).isFalse()
        }

        verify("first txHash remains in database") {
            val result = repository.getById(id)

            expectThat(result)
                .isEqualTo(
                    AssetSendRequest(
                        id = id,
                        projectId = PROJECT_ID,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        tokenAddress = TOKEN_ADDRESS,
                        assetAmount = ASSET_AMOUNT,
                        assetSenderAddress = ASSET_SENDER_ADDRESS,
                        assetRecipientAddress = ASSET_RECIPIENT_ADDRESS,
                        txHash = TX_HASH,
                        arbitraryData = ARBITRARY_DATA,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = SEND_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = SEND_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }
}
