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
import polycode.features.contract.arbitrarycall.model.filters.ContractArbitraryCallRequestFilters
import polycode.features.contract.arbitrarycall.model.params.StoreContractArbitraryCallRequestParams
import polycode.features.contract.arbitrarycall.model.result.ContractArbitraryCallRequest
import polycode.features.contract.arbitrarycall.repository.JooqContractArbitraryCallRequestRepository
import polycode.generated.jooq.enums.UserIdentifierType
import polycode.generated.jooq.id.ContractArbitraryCallRequestId
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.generated.jooq.id.ContractMetadataId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import polycode.generated.jooq.tables.records.ContractArbitraryCallRequestRecord
import polycode.generated.jooq.tables.records.ContractDeploymentRequestRecord
import polycode.generated.jooq.tables.records.ContractMetadataRecord
import polycode.generated.jooq.tables.records.ProjectRecord
import polycode.generated.jooq.tables.records.UserIdentifierRecord
import polycode.model.ScreenConfig
import polycode.testcontainers.SharedTestContainers
import polycode.util.Balance
import polycode.util.BaseUrl
import polycode.util.ChainId
import polycode.util.Constants
import polycode.util.ContractAddress
import polycode.util.ContractBinaryData
import polycode.util.ContractId
import polycode.util.FunctionData
import polycode.util.TransactionHash
import polycode.util.WalletAddress
import java.math.BigInteger
import java.util.UUID

@JooqTest
@Import(JooqContractArbitraryCallRequestRepository::class, DatabaseConfig::class)
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqContractArbitraryCallRequestRepositoryIntegTest : TestBase() {

    companion object {
        private val PROJECT_ID_1 = ProjectId(UUID.randomUUID())
        private val PROJECT_ID_2 = ProjectId(UUID.randomUUID())
        private val OWNER_ID = UserId(UUID.randomUUID())
        private val DEPLOYED_CONTRACT_ID = ContractDeploymentRequestId(UUID.randomUUID())
        private val CONTRACT_ADDRESS = ContractAddress("1337")
        private const val FUNCTION_NAME = "balanceOf"
        private val FUNCTION_DATA = FunctionData("test")
        private val ETH_AMOUNT = Balance(BigInteger("10000"))
        private val CHAIN_ID = ChainId(1337L)
        private const val REDIRECT_URL = "redirect-url"
        private val ARBITRARY_DATA = TestData.EMPTY_JSON_OBJECT
        private const val SCREEN_BEFORE_ACTION_MESSAGE = "screen-before-action-message"
        private const val SCREEN_AFTER_ACTION_MESSAGE = "screen-after-action-message"
        private val CALLER_ADDRESS = WalletAddress("123")
        private val TX_HASH = TransactionHash("tx-hash")
    }

    @Suppress("unused")
    private val postgresContainer = SharedTestContainers.postgresContainer

    @Autowired
    private lateinit var repository: JooqContractArbitraryCallRequestRepository

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
                id = PROJECT_ID_1,
                ownerId = OWNER_ID,
                baseRedirectUrl = BaseUrl("base-redirect-url"),
                chainId = ChainId(1337L),
                customRpcUrl = "custom-rpc-url",
                createdAt = TestData.TIMESTAMP
            )
        )

        dslContext.executeInsert(
            ProjectRecord(
                id = PROJECT_ID_2,
                ownerId = OWNER_ID,
                baseRedirectUrl = BaseUrl("base-redirect-url"),
                chainId = ChainId(1337L),
                customRpcUrl = "custom-rpc-url",
                createdAt = TestData.TIMESTAMP
            )
        )

        val metadataId = ContractMetadataId(UUID.randomUUID())

        dslContext.executeInsert(
            ContractMetadataRecord(
                id = metadataId,
                contractId = ContractId("contract-id"),
                contractTags = emptyArray(),
                contractImplements = emptyArray(),
                name = null,
                description = null,
                projectId = Constants.NIL_PROJECT_ID
            )
        )

        dslContext.executeInsert(
            ContractDeploymentRequestRecord(
                id = DEPLOYED_CONTRACT_ID,
                alias = ContractArbitraryCallRequestId(UUID.randomUUID()).toString(),
                contractMetadataId = metadataId,
                contractData = ContractBinaryData("00"),
                constructorParams = TestData.EMPTY_JSON_ARRAY,
                initialEthAmount = Balance.ZERO,
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                projectId = PROJECT_ID_1,
                createdAt = TestData.TIMESTAMP,
                arbitraryData = ARBITRARY_DATA,
                screenBeforeActionMessage = SCREEN_BEFORE_ACTION_MESSAGE,
                screenAfterActionMessage = SCREEN_AFTER_ACTION_MESSAGE,
                contractAddress = CONTRACT_ADDRESS,
                deployerAddress = CALLER_ADDRESS,
                txHash = TX_HASH,
                imported = false,
                deleted = false,
                proxy = false,
                implementationContractAddress = null
            )
        )
    }

    @Test
    fun mustCorrectlyFetchContractArbitraryCallRequestById() {
        val id = ContractArbitraryCallRequestId(UUID.randomUUID())
        val record = createRecord(id)

        suppose("some contract arbitrary call request exists in database") {
            dslContext.executeInsert(record)
        }

        verify("contract arbitrary call request is correctly fetched by ID") {
            val result = repository.getById(id)

            expectThat(result)
                .isEqualTo(record.toModel())
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentContractArbitraryCallRequestById() {
        verify("null is returned when fetching non-existent contract arbitrary call request") {
            val result = repository.getById(ContractArbitraryCallRequestId(UUID.randomUUID()))

            expectThat(result)
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyFetchContractArbitraryCallRequestsByProjectIdAndFilters() {
        val project1ContractsWithMatchingDeployedContractIdAndAddress = listOf(
            createRecord(id = ContractArbitraryCallRequestId(UUID.randomUUID())),
            createRecord(id = ContractArbitraryCallRequestId(UUID.randomUUID()))
        )
        val project1ContractsWithMissingDeployedContractId = listOf(
            createRecord(id = ContractArbitraryCallRequestId(UUID.randomUUID()), deployedContractId = null),
            createRecord(id = ContractArbitraryCallRequestId(UUID.randomUUID()), deployedContractId = null)
        )
        val project1ContractsWithNonMatchingContractAddress = listOf(
            createRecord(
                id = ContractArbitraryCallRequestId(UUID.randomUUID()),
                contractAddress = ContractAddress("dead")
            ),
            createRecord(
                id = ContractArbitraryCallRequestId(UUID.randomUUID()),
                contractAddress = ContractAddress("dead")
            )
        )
        val project2ContractsWithMatchingDeployedContractIdAndAddress = listOf(
            createRecord(id = ContractArbitraryCallRequestId(UUID.randomUUID()), projectId = PROJECT_ID_2),
            createRecord(id = ContractArbitraryCallRequestId(UUID.randomUUID()), projectId = PROJECT_ID_2)
        )
        val project2ContractsWithMissingDeployedContractId = listOf(
            createRecord(
                id = ContractArbitraryCallRequestId(UUID.randomUUID()),
                deployedContractId = null,
                projectId = PROJECT_ID_2
            ),
            createRecord(
                id = ContractArbitraryCallRequestId(UUID.randomUUID()),
                deployedContractId = null,
                projectId = PROJECT_ID_2
            )
        )
        val project2ContractsWithNonMatchingContractAddress = listOf(
            createRecord(
                id = ContractArbitraryCallRequestId(UUID.randomUUID()),
                contractAddress = ContractAddress("dead"),
                projectId = PROJECT_ID_2
            ),
            createRecord(
                id = ContractArbitraryCallRequestId(UUID.randomUUID()),
                contractAddress = ContractAddress("dead"),
                projectId = PROJECT_ID_2
            )
        )

        suppose("some contract arbitrary call requests exist in database") {
            dslContext.batchInsert(
                project1ContractsWithMatchingDeployedContractIdAndAddress +
                    project1ContractsWithMissingDeployedContractId + project1ContractsWithNonMatchingContractAddress +
                    project2ContractsWithMatchingDeployedContractIdAndAddress +
                    project2ContractsWithMissingDeployedContractId + project2ContractsWithNonMatchingContractAddress
            ).execute()
        }

        verify("must correctly fetch project 1 contract arbitrary calls with matching contract ID and address") {
            expectThat(
                repository.getAllByProjectId(
                    projectId = PROJECT_ID_1,
                    filters = ContractArbitraryCallRequestFilters(
                        deployedContractId = DEPLOYED_CONTRACT_ID,
                        contractAddress = CONTRACT_ADDRESS
                    )
                )
            ).containsExactlyInAnyOrderElementsOf(
                models(project1ContractsWithMatchingDeployedContractIdAndAddress)
            )
        }

        verify("must correctly fetch project 1 contract arbitrary calls with matching contract ID") {
            expectThat(
                repository.getAllByProjectId(
                    projectId = PROJECT_ID_1,
                    filters = ContractArbitraryCallRequestFilters(
                        deployedContractId = DEPLOYED_CONTRACT_ID,
                        contractAddress = null
                    )
                )
            ).containsExactlyInAnyOrderElementsOf(
                models(
                    project1ContractsWithMatchingDeployedContractIdAndAddress,
                    project1ContractsWithNonMatchingContractAddress
                )
            )
        }

        verify("must correctly fetch project 1 contract arbitrary calls with matching contract address") {
            expectThat(
                repository.getAllByProjectId(
                    projectId = PROJECT_ID_1,
                    filters = ContractArbitraryCallRequestFilters(
                        deployedContractId = null,
                        contractAddress = CONTRACT_ADDRESS
                    )
                )
            ).containsExactlyInAnyOrderElementsOf(
                models(
                    project1ContractsWithMatchingDeployedContractIdAndAddress,
                    project1ContractsWithMissingDeployedContractId
                )
            )
        }

        verify("must correctly fetch all project 1 contract arbitrary calls") {
            expectThat(
                repository.getAllByProjectId(
                    projectId = PROJECT_ID_1,
                    filters = ContractArbitraryCallRequestFilters(
                        deployedContractId = null,
                        contractAddress = null
                    )
                )
            ).containsExactlyInAnyOrderElementsOf(
                models(
                    project1ContractsWithMatchingDeployedContractIdAndAddress,
                    project1ContractsWithMissingDeployedContractId,
                    project1ContractsWithNonMatchingContractAddress
                )
            )
        }

        verify("must correctly fetch project 2 contract arbitrary calls with matching contract ID and address") {
            expectThat(
                repository.getAllByProjectId(
                    projectId = PROJECT_ID_2,
                    filters = ContractArbitraryCallRequestFilters(
                        deployedContractId = DEPLOYED_CONTRACT_ID,
                        contractAddress = CONTRACT_ADDRESS
                    )
                )
            ).containsExactlyInAnyOrderElementsOf(
                models(project2ContractsWithMatchingDeployedContractIdAndAddress)
            )
        }

        verify("must correctly fetch project 2 contract arbitrary calls with matching contract ID") {
            expectThat(
                repository.getAllByProjectId(
                    projectId = PROJECT_ID_2,
                    filters = ContractArbitraryCallRequestFilters(
                        deployedContractId = DEPLOYED_CONTRACT_ID,
                        contractAddress = null
                    )
                )
            ).containsExactlyInAnyOrderElementsOf(
                models(
                    project2ContractsWithMatchingDeployedContractIdAndAddress,
                    project2ContractsWithNonMatchingContractAddress
                )
            )
        }

        verify("must correctly fetch project 2 contract arbitrary calls with matching contract address") {
            expectThat(
                repository.getAllByProjectId(
                    projectId = PROJECT_ID_2,
                    filters = ContractArbitraryCallRequestFilters(
                        deployedContractId = null,
                        contractAddress = CONTRACT_ADDRESS
                    )
                )
            ).containsExactlyInAnyOrderElementsOf(
                models(
                    project2ContractsWithMatchingDeployedContractIdAndAddress,
                    project2ContractsWithMissingDeployedContractId
                )
            )
        }

        verify("must correctly fetch all project 2 contract arbitrary calls") {
            expectThat(
                repository.getAllByProjectId(
                    projectId = PROJECT_ID_2,
                    filters = ContractArbitraryCallRequestFilters(
                        deployedContractId = null,
                        contractAddress = null
                    )
                )
            ).containsExactlyInAnyOrderElementsOf(
                models(
                    project2ContractsWithMatchingDeployedContractIdAndAddress,
                    project2ContractsWithMissingDeployedContractId,
                    project2ContractsWithNonMatchingContractAddress
                )
            )
        }
    }

    @Test
    fun mustCorrectlyStoreContractArbitraryCallRequest() {
        val id = ContractArbitraryCallRequestId(UUID.randomUUID())
        val params = StoreContractArbitraryCallRequestParams(
            id = id,
            deployedContractId = DEPLOYED_CONTRACT_ID,
            contractAddress = CONTRACT_ADDRESS,
            functionData = FUNCTION_DATA,
            functionName = FUNCTION_NAME,
            functionParams = TestData.EMPTY_JSON_ARRAY,
            ethAmount = ETH_AMOUNT,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            projectId = PROJECT_ID_1,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SCREEN_AFTER_ACTION_MESSAGE
            ),
            callerAddress = CALLER_ADDRESS
        )

        val storedContractArbitraryCallRequest = suppose("contract arbitrary call request is stored in database") {
            repository.store(params)
        }

        val expectedContractArbitraryCallRequest = ContractArbitraryCallRequest(
            id = id,
            deployedContractId = DEPLOYED_CONTRACT_ID,
            contractAddress = CONTRACT_ADDRESS,
            functionData = FUNCTION_DATA,
            functionName = FUNCTION_NAME,
            functionParams = TestData.EMPTY_JSON_ARRAY,
            ethAmount = ETH_AMOUNT,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            projectId = PROJECT_ID_1,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SCREEN_AFTER_ACTION_MESSAGE
            ),
            callerAddress = CALLER_ADDRESS,
            txHash = null
        )

        verify("storing contract arbitrary call request returns correct result") {
            expectThat(storedContractArbitraryCallRequest)
                .isEqualTo(expectedContractArbitraryCallRequest)
        }

        verify("contract arbitrary call request was stored in database") {
            val result = repository.getById(id)

            expectThat(result)
                .isEqualTo(expectedContractArbitraryCallRequest)
        }
    }

    @Test
    fun mustCorrectlySetTxInfoForContractArbitraryCallRequestWithNullTxHash() {
        val id = ContractArbitraryCallRequestId(UUID.randomUUID())
        val params = StoreContractArbitraryCallRequestParams(
            id = id,
            deployedContractId = DEPLOYED_CONTRACT_ID,
            contractAddress = CONTRACT_ADDRESS,
            functionData = FUNCTION_DATA,
            functionName = FUNCTION_NAME,
            functionParams = TestData.EMPTY_JSON_ARRAY,
            ethAmount = ETH_AMOUNT,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            projectId = PROJECT_ID_1,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SCREEN_AFTER_ACTION_MESSAGE
            ),
            callerAddress = null
        )

        suppose("contract arbitrary call request is stored in database") {
            repository.store(params)
        }

        verify("setting txInfo will succeed") {
            expectThat(repository.setTxInfo(id, TX_HASH, CALLER_ADDRESS))
                .isTrue()
        }

        verify("txInfo is correctly set in database") {
            val result = repository.getById(id)

            expectThat(result)
                .isEqualTo(
                    ContractArbitraryCallRequest(
                        id = id,
                        deployedContractId = DEPLOYED_CONTRACT_ID,
                        contractAddress = CONTRACT_ADDRESS,
                        functionData = FUNCTION_DATA,
                        functionName = FUNCTION_NAME,
                        functionParams = TestData.EMPTY_JSON_ARRAY,
                        ethAmount = ETH_AMOUNT,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        projectId = PROJECT_ID_1,
                        createdAt = TestData.TIMESTAMP,
                        arbitraryData = ARBITRARY_DATA,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        callerAddress = CALLER_ADDRESS,
                        txHash = TX_HASH,
                    )
                )
        }
    }

    @Test
    fun mustNotUpdateCallerAddressForContractArbitraryCallRequestWhenCallerIsAlreadySet() {
        val id = ContractArbitraryCallRequestId(UUID.randomUUID())
        val params = StoreContractArbitraryCallRequestParams(
            id = id,
            deployedContractId = DEPLOYED_CONTRACT_ID,
            contractAddress = CONTRACT_ADDRESS,
            functionData = FUNCTION_DATA,
            functionName = FUNCTION_NAME,
            functionParams = TestData.EMPTY_JSON_ARRAY,
            ethAmount = ETH_AMOUNT,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            projectId = PROJECT_ID_1,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SCREEN_AFTER_ACTION_MESSAGE
            ),
            callerAddress = CALLER_ADDRESS
        )

        suppose("contract arbitrary call request is stored in database") {
            repository.store(params)
        }

        verify("setting txInfo will succeed") {
            val ignoredCaller = WalletAddress("f")
            expectThat(repository.setTxInfo(id, TX_HASH, ignoredCaller))
                .isTrue()
        }

        verify("txHash was correctly set while contract arbitrary caller was not updated") {
            val result = repository.getById(id)

            expectThat(result)
                .isEqualTo(
                    ContractArbitraryCallRequest(
                        id = id,
                        deployedContractId = DEPLOYED_CONTRACT_ID,
                        contractAddress = CONTRACT_ADDRESS,
                        functionData = FUNCTION_DATA,
                        functionName = FUNCTION_NAME,
                        functionParams = TestData.EMPTY_JSON_ARRAY,
                        ethAmount = ETH_AMOUNT,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        projectId = PROJECT_ID_1,
                        createdAt = TestData.TIMESTAMP,
                        arbitraryData = ARBITRARY_DATA,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        callerAddress = CALLER_ADDRESS,
                        txHash = TX_HASH,
                    )
                )
        }
    }

    @Test
    fun mustNotSetTxInfoForContractArbitraryCallRequestWhenTxHashIsAlreadySet() {
        val id = ContractArbitraryCallRequestId(UUID.randomUUID())
        val params = StoreContractArbitraryCallRequestParams(
            id = id,
            deployedContractId = DEPLOYED_CONTRACT_ID,
            contractAddress = CONTRACT_ADDRESS,
            functionData = FUNCTION_DATA,
            functionName = FUNCTION_NAME,
            functionParams = TestData.EMPTY_JSON_ARRAY,
            ethAmount = ETH_AMOUNT,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            projectId = PROJECT_ID_1,
            createdAt = TestData.TIMESTAMP,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SCREEN_AFTER_ACTION_MESSAGE
            ),
            callerAddress = null
        )

        suppose("contract arbitrary call request is stored in database") {
            repository.store(params)
        }

        verify("setting txInfo will succeed") {
            expectThat(repository.setTxInfo(id, TX_HASH, CALLER_ADDRESS))
                .isTrue()
        }

        verify("setting another txInfo will not succeed") {
            expectThat(
                repository.setTxInfo(
                    id = id,
                    txHash = TransactionHash("different-tx-hash"),
                    caller = CALLER_ADDRESS
                )
            ).isFalse()
        }

        verify("first txHash remains in database") {
            val result = repository.getById(id)

            expectThat(result)
                .isEqualTo(
                    ContractArbitraryCallRequest(
                        id = id,
                        deployedContractId = DEPLOYED_CONTRACT_ID,
                        contractAddress = CONTRACT_ADDRESS,
                        functionData = FUNCTION_DATA,
                        functionName = FUNCTION_NAME,
                        functionParams = TestData.EMPTY_JSON_ARRAY,
                        ethAmount = ETH_AMOUNT,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        projectId = PROJECT_ID_1,
                        createdAt = TestData.TIMESTAMP,
                        arbitraryData = ARBITRARY_DATA,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        callerAddress = CALLER_ADDRESS,
                        txHash = TX_HASH,
                    )
                )
        }
    }

    private fun createRecord(
        id: ContractArbitraryCallRequestId,
        deployedContractId: ContractDeploymentRequestId? = DEPLOYED_CONTRACT_ID,
        contractAddress: ContractAddress = CONTRACT_ADDRESS,
        projectId: ProjectId = PROJECT_ID_1
    ) = ContractArbitraryCallRequestRecord(
        id = id,
        deployedContractId = deployedContractId,
        contractAddress = contractAddress,
        functionData = FUNCTION_DATA,
        functionName = FUNCTION_NAME,
        functionParams = TestData.EMPTY_JSON_ARRAY,
        ethAmount = ETH_AMOUNT,
        chainId = CHAIN_ID,
        redirectUrl = REDIRECT_URL,
        projectId = projectId,
        createdAt = TestData.TIMESTAMP,
        arbitraryData = ARBITRARY_DATA,
        screenBeforeActionMessage = SCREEN_BEFORE_ACTION_MESSAGE,
        screenAfterActionMessage = SCREEN_AFTER_ACTION_MESSAGE,
        callerAddress = CALLER_ADDRESS,
        txHash = TX_HASH
    )

    private fun ContractArbitraryCallRequestRecord.toModel() =
        ContractArbitraryCallRequest(
            id = id,
            deployedContractId = deployedContractId,
            contractAddress = contractAddress,
            functionData = functionData,
            functionName = functionName,
            functionParams = functionParams,
            ethAmount = ethAmount,
            chainId = chainId,
            redirectUrl = redirectUrl,
            projectId = projectId,
            createdAt = createdAt,
            arbitraryData = arbitraryData,
            screenConfig = ScreenConfig(
                beforeActionMessage = screenBeforeActionMessage,
                afterActionMessage = screenAfterActionMessage
            ),
            callerAddress = callerAddress,
            txHash = txHash
        )

    private fun models(vararg records: List<ContractArbitraryCallRequestRecord>): List<ContractArbitraryCallRequest> =
        records.flatMap { it }.map { it.toModel() }
}
