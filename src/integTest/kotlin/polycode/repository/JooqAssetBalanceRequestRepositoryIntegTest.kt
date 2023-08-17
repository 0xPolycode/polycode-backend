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
import polycode.features.asset.balance.model.params.StoreAssetBalanceRequestParams
import polycode.features.asset.balance.model.result.AssetBalanceRequest
import polycode.features.asset.balance.repository.JooqAssetBalanceRequestRepository
import polycode.generated.jooq.enums.UserIdentifierType
import polycode.generated.jooq.id.AssetBalanceRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import polycode.generated.jooq.tables.records.AssetBalanceRequestRecord
import polycode.generated.jooq.tables.records.ProjectRecord
import polycode.generated.jooq.tables.records.UserIdentifierRecord
import polycode.model.ScreenConfig
import polycode.testcontainers.SharedTestContainers
import polycode.util.BaseUrl
import polycode.util.BlockNumber
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.SignedMessage
import polycode.util.WalletAddress
import java.math.BigInteger
import java.util.UUID

@JooqTest
@Import(JooqAssetBalanceRequestRepository::class, DatabaseConfig::class)
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqAssetBalanceRequestRepositoryIntegTest : TestBase() {

    companion object {
        private val CHAIN_ID = ChainId(1337L)
        private const val REDIRECT_URL = "redirect-url"
        private val TOKEN_ADDRESS = ContractAddress("a")
        private val BLOCK_NUMBER = BlockNumber(BigInteger.valueOf(123L))
        private val REQUESTED_WALLET_ADDRESS = WalletAddress("b")
        private val ARBITRARY_DATA = TestData.EMPTY_JSON_OBJECT
        private const val BALANCE_SCREEN_BEFORE_ACTION_MESSAGE = "balance-screen-before-action-message"
        private const val BALANCE_SCREEN_AFTER_ACTION_MESSAGE = "balance-screen-after-action-message"
        private val ACTUAL_WALLET_ADDRESS = WalletAddress("c")
        private val SIGNED_MESSAGE = SignedMessage("signed-message")
        private val PROJECT_ID = ProjectId(UUID.randomUUID())
        private val OWNER_ID = UserId(UUID.randomUUID())
    }

    @Suppress("unused")
    private val postgresContainer = SharedTestContainers.postgresContainer

    @Autowired
    private lateinit var repository: JooqAssetBalanceRequestRepository

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
    fun mustCorrectlyFetchAssetBalanceRequestById() {
        val id = AssetBalanceRequestId(UUID.randomUUID())

        suppose("some asset balance request exists in database") {
            dslContext.executeInsert(
                AssetBalanceRequestRecord(
                    id = id,
                    projectId = PROJECT_ID,
                    chainId = CHAIN_ID,
                    redirectUrl = REDIRECT_URL,
                    tokenAddress = TOKEN_ADDRESS,
                    blockNumber = BLOCK_NUMBER,
                    requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
                    arbitraryData = ARBITRARY_DATA,
                    screenBeforeActionMessage = BALANCE_SCREEN_BEFORE_ACTION_MESSAGE,
                    screenAfterActionMessage = BALANCE_SCREEN_AFTER_ACTION_MESSAGE,
                    actualWalletAddress = ACTUAL_WALLET_ADDRESS,
                    signedMessage = SIGNED_MESSAGE,
                    createdAt = TestData.TIMESTAMP
                )
            )
        }

        verify("asset balance request is correctly fetched by ID") {
            val result = repository.getById(id)

            expectThat(result)
                .isEqualTo(
                    AssetBalanceRequest(
                        id = id,
                        projectId = PROJECT_ID,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        tokenAddress = TOKEN_ADDRESS,
                        blockNumber = BLOCK_NUMBER,
                        requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
                        actualWalletAddress = ACTUAL_WALLET_ADDRESS,
                        signedMessage = SIGNED_MESSAGE,
                        arbitraryData = ARBITRARY_DATA,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = BALANCE_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = BALANCE_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentAssetBalanceRequestById() {
        verify("null is returned when fetching non-existent asset balance request") {
            val result = repository.getById(AssetBalanceRequestId(UUID.randomUUID()))

            expectThat(result)
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyFetchAssetLockRequestsByProject() {
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
            AssetBalanceRequestRecord(
                id = AssetBalanceRequestId(UUID.randomUUID()),
                projectId = PROJECT_ID,
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                blockNumber = BLOCK_NUMBER,
                requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
                arbitraryData = ARBITRARY_DATA,
                screenBeforeActionMessage = BALANCE_SCREEN_BEFORE_ACTION_MESSAGE,
                screenAfterActionMessage = BALANCE_SCREEN_AFTER_ACTION_MESSAGE,
                actualWalletAddress = ACTUAL_WALLET_ADDRESS,
                signedMessage = SIGNED_MESSAGE,
                createdAt = TestData.TIMESTAMP
            ),
            AssetBalanceRequestRecord(
                id = AssetBalanceRequestId(UUID.randomUUID()),
                projectId = PROJECT_ID,
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                blockNumber = BLOCK_NUMBER,
                requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
                arbitraryData = ARBITRARY_DATA,
                screenBeforeActionMessage = BALANCE_SCREEN_BEFORE_ACTION_MESSAGE,
                screenAfterActionMessage = BALANCE_SCREEN_AFTER_ACTION_MESSAGE,
                actualWalletAddress = ACTUAL_WALLET_ADDRESS,
                signedMessage = SIGNED_MESSAGE,
                createdAt = TestData.TIMESTAMP
            )
        )
        val otherRequests = listOf(
            AssetBalanceRequestRecord(
                id = AssetBalanceRequestId(UUID.randomUUID()),
                projectId = otherProjectId,
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                blockNumber = BLOCK_NUMBER,
                requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
                arbitraryData = ARBITRARY_DATA,
                screenBeforeActionMessage = BALANCE_SCREEN_BEFORE_ACTION_MESSAGE,
                screenAfterActionMessage = BALANCE_SCREEN_AFTER_ACTION_MESSAGE,
                actualWalletAddress = ACTUAL_WALLET_ADDRESS,
                signedMessage = SIGNED_MESSAGE,
                createdAt = TestData.TIMESTAMP
            ),
            AssetBalanceRequestRecord(
                id = AssetBalanceRequestId(UUID.randomUUID()),
                projectId = otherProjectId,
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                blockNumber = BLOCK_NUMBER,
                requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
                arbitraryData = ARBITRARY_DATA,
                screenBeforeActionMessage = BALANCE_SCREEN_BEFORE_ACTION_MESSAGE,
                screenAfterActionMessage = BALANCE_SCREEN_AFTER_ACTION_MESSAGE,
                actualWalletAddress = ACTUAL_WALLET_ADDRESS,
                signedMessage = SIGNED_MESSAGE,
                createdAt = TestData.TIMESTAMP
            )
        )

        suppose("some asset balance requests exist in database") {
            dslContext.batchInsert(projectRequests + otherRequests).execute()
        }

        verify("asset balance requests are correctly fetched by project") {
            val result = repository.getAllByProjectId(PROJECT_ID)

            expectThat(result)
                .containsExactlyInAnyOrderElementsOf(
                    projectRequests.map {
                        AssetBalanceRequest(
                            id = it.id,
                            projectId = it.projectId,
                            chainId = it.chainId,
                            redirectUrl = it.redirectUrl,
                            tokenAddress = it.tokenAddress,
                            blockNumber = it.blockNumber,
                            requestedWalletAddress = it.requestedWalletAddress,
                            actualWalletAddress = it.actualWalletAddress,
                            signedMessage = it.signedMessage,
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
    fun mustCorrectlyStoreAssetBalanceRequest() {
        val id = AssetBalanceRequestId(UUID.randomUUID())
        val params = StoreAssetBalanceRequestParams(
            id = id,
            projectId = PROJECT_ID,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            blockNumber = BLOCK_NUMBER,
            requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = BALANCE_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = BALANCE_SCREEN_AFTER_ACTION_MESSAGE
            ),
            createdAt = TestData.TIMESTAMP
        )

        val storedAssetBalanceRequest = suppose("asset balance request is stored in database") {
            repository.store(params)
        }

        val expectedAssetBalanceRequest = AssetBalanceRequest(
            id = id,
            projectId = PROJECT_ID,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            blockNumber = BLOCK_NUMBER,
            requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
            actualWalletAddress = null,
            signedMessage = null,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = BALANCE_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = BALANCE_SCREEN_AFTER_ACTION_MESSAGE
            ),
            createdAt = TestData.TIMESTAMP
        )

        verify("storing asset balance request returns correct result") {
            expectThat(storedAssetBalanceRequest)
                .isEqualTo(expectedAssetBalanceRequest)
        }

        verify("asset balance request was stored in database") {
            val result = repository.getById(id)

            expectThat(result)
                .isEqualTo(expectedAssetBalanceRequest)
        }
    }

    @Test
    fun mustCorrectlySetSignedMessageForAssetBalanceRequestWithNullWalletAddressAndSignedMessage() {
        val id = AssetBalanceRequestId(UUID.randomUUID())
        val params = StoreAssetBalanceRequestParams(
            id = id,
            projectId = PROJECT_ID,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            blockNumber = BLOCK_NUMBER,
            requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = BALANCE_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = BALANCE_SCREEN_AFTER_ACTION_MESSAGE
            ),
            createdAt = TestData.TIMESTAMP
        )

        suppose("asset balance request is stored in database") {
            repository.store(params)
        }

        verify("setting walletAddress and signedMessage will succeed") {
            expectThat(repository.setSignedMessage(id, ACTUAL_WALLET_ADDRESS, SIGNED_MESSAGE))
                .isTrue()
        }

        verify("walletAddress and signedMessage were correctly set in database") {
            val result = repository.getById(id)

            expectThat(result)
                .isEqualTo(
                    AssetBalanceRequest(
                        id = id,
                        projectId = PROJECT_ID,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        tokenAddress = TOKEN_ADDRESS,
                        blockNumber = BLOCK_NUMBER,
                        requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
                        actualWalletAddress = ACTUAL_WALLET_ADDRESS,
                        signedMessage = SIGNED_MESSAGE,
                        arbitraryData = ARBITRARY_DATA,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = BALANCE_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = BALANCE_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustNotSetWalletAddressAndSignedMessageForAssetBalanceRequestWhenWalletAddressAndSignedMessageAreAlreadySet() {
        val id = AssetBalanceRequestId(UUID.randomUUID())
        val params = StoreAssetBalanceRequestParams(
            id = id,
            projectId = PROJECT_ID,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            blockNumber = BLOCK_NUMBER,
            requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = BALANCE_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = BALANCE_SCREEN_AFTER_ACTION_MESSAGE
            ),
            createdAt = TestData.TIMESTAMP
        )

        suppose("asset balance request is stored in database") {
            repository.store(params)
        }

        verify("setting walletAddress and signedMessage will succeed") {
            expectThat(repository.setSignedMessage(id, ACTUAL_WALLET_ADDRESS, SIGNED_MESSAGE))
                .isTrue()
        }

        verify("setting another walletAddress and signedMessage will not succeed") {
            expectThat(repository.setSignedMessage(id, WalletAddress("dead"), SignedMessage("another-message")))
                .isFalse()
        }

        verify("first walletAddress and signedMessage remain in database") {
            val result = repository.getById(id)

            expectThat(result)
                .isEqualTo(
                    AssetBalanceRequest(
                        id = id,
                        projectId = PROJECT_ID,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        tokenAddress = TOKEN_ADDRESS,
                        blockNumber = BLOCK_NUMBER,
                        requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
                        actualWalletAddress = ACTUAL_WALLET_ADDRESS,
                        signedMessage = SIGNED_MESSAGE,
                        arbitraryData = ARBITRARY_DATA,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = BALANCE_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = BALANCE_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }
}
