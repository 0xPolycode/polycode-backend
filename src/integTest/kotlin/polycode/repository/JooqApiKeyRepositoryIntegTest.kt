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
import polycode.features.api.access.model.result.ApiKey
import polycode.features.api.access.repository.JooqApiKeyRepository
import polycode.generated.jooq.enums.UserIdentifierType
import polycode.generated.jooq.id.ApiKeyId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import polycode.generated.jooq.tables.records.ApiKeyRecord
import polycode.generated.jooq.tables.records.ProjectRecord
import polycode.generated.jooq.tables.records.UserIdentifierRecord
import polycode.testcontainers.SharedTestContainers
import polycode.util.BaseUrl
import polycode.util.ChainId
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@JooqTest
@Import(JooqApiKeyRepository::class, DatabaseConfig::class)
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqApiKeyRepositoryIntegTest : TestBase() {

    companion object {
        private val PROJECT_ID = ProjectId(UUID.randomUUID())
        private val OWNER_ID = UserId(UUID.randomUUID())
        private const val API_KEY = "api-key"
    }

    @Suppress("unused")
    private val postgresContainer = SharedTestContainers.postgresContainer

    @Autowired
    private lateinit var repository: JooqApiKeyRepository

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
    fun mustCorrectlyFetchApiKeyById() {
        val id = ApiKeyId(UUID.randomUUID())

        suppose("some API key is stored in database") {
            dslContext.executeInsert(
                ApiKeyRecord(
                    id = id,
                    projectId = PROJECT_ID,
                    apiKey = API_KEY,
                    createdAt = TestData.TIMESTAMP
                )
            )
        }

        verify("API key is correctly fetched by ID") {
            val result = repository.getById(id)

            expectThat(result)
                .isEqualTo(
                    ApiKey(
                        id = id,
                        projectId = PROJECT_ID,
                        apiKey = API_KEY,
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchApiKeyByValue() {
        val id = ApiKeyId(UUID.randomUUID())

        suppose("some API key is stored in database") {
            dslContext.executeInsert(
                ApiKeyRecord(
                    id = id,
                    projectId = PROJECT_ID,
                    apiKey = API_KEY,
                    createdAt = TestData.TIMESTAMP
                )
            )
        }

        verify("API key is correctly fetched by ID") {
            val result = repository.getByValue(API_KEY)

            expectThat(result)
                .isEqualTo(
                    ApiKey(
                        id = id,
                        projectId = PROJECT_ID,
                        apiKey = API_KEY,
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentApiKeyById() {
        verify("null is returned when fetching non-existent API key") {
            val result = repository.getById(ApiKeyId(UUID.randomUUID()))

            expectThat(result)
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyFetchAllApiKeysForSomeProject() {
        suppose("some API keys are stored in database") {
            dslContext.batchInsert(
                ApiKeyRecord(
                    id = ApiKeyId(UUID.randomUUID()),
                    projectId = PROJECT_ID,
                    apiKey = "api-key-1",
                    createdAt = TestData.TIMESTAMP
                ),
                ApiKeyRecord(
                    id = ApiKeyId(UUID.randomUUID()),
                    projectId = PROJECT_ID,
                    apiKey = "api-key-2",
                    createdAt = TestData.TIMESTAMP + 10.seconds
                )
            ).execute()
        }

        verify("API keys are correctly fetched by project ID") {
            val result = repository.getAllByProjectId(PROJECT_ID)

            expectThat(result.map { it.apiKey })
                .isEqualTo(
                    listOf("api-key-1", "api-key-2")
                )
        }
    }

    @Test
    fun mustCorrectlyDetermineIfApiKeyExists() {
        suppose("some API key is stored in database") {
            dslContext.executeInsert(
                ApiKeyRecord(
                    id = ApiKeyId(UUID.randomUUID()),
                    projectId = PROJECT_ID,
                    apiKey = API_KEY,
                    createdAt = TestData.TIMESTAMP
                )
            )
        }

        verify("must correctly determine that API key exists") {
            expectThat(repository.exists(API_KEY))
                .isTrue()
            expectThat(repository.exists("unknown-api-key"))
                .isFalse()
        }
    }

    @Test
    fun mustCorrectlyStoreApiKey() {
        val id = ApiKeyId(UUID.randomUUID())
        val apiKey = ApiKey(
            id = id,
            projectId = PROJECT_ID,
            apiKey = API_KEY,
            createdAt = TestData.TIMESTAMP
        )

        val storedApiKey = suppose("API key is stored in database") {
            repository.store(apiKey)
        }

        verify("storing API key returns correct result") {
            expectThat(storedApiKey)
                .isEqualTo(apiKey)
        }

        verify("API key was stored in database") {
            val result = repository.getById(id)

            expectThat(result)
                .isEqualTo(apiKey)
        }
    }
}
