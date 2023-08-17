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
import polycode.features.api.access.model.result.Project
import polycode.features.api.access.repository.JooqProjectRepository
import polycode.generated.jooq.enums.UserIdentifierType
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import polycode.generated.jooq.tables.records.ProjectRecord
import polycode.generated.jooq.tables.records.UserIdentifierRecord
import polycode.testcontainers.SharedTestContainers
import polycode.util.BaseUrl
import polycode.util.ChainId
import java.util.UUID

@JooqTest
@Import(JooqProjectRepository::class, DatabaseConfig::class)
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqProjectRepositoryIntegTest : TestBase() {

    companion object {
        private val OWNER_ID = UserId(UUID.randomUUID())
        private val BASE_REDIRECT_URL = BaseUrl("base-redirect-url")
        private val CHAIN_ID = ChainId(1337L)
        private const val CUSTOM_RPC_URL = "custom-rpc-url"
    }

    @Suppress("unused")
    private val postgresContainer = SharedTestContainers.postgresContainer

    @Autowired
    private lateinit var repository: JooqProjectRepository

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
    }

    @Test
    fun mustCorrectlyFetchProjectById() {
        val id = ProjectId(UUID.randomUUID())

        suppose("some project is stored in database") {
            dslContext.executeInsert(
                ProjectRecord(
                    id = id,
                    ownerId = OWNER_ID,
                    baseRedirectUrl = BASE_REDIRECT_URL,
                    chainId = CHAIN_ID,
                    customRpcUrl = CUSTOM_RPC_URL,
                    createdAt = TestData.TIMESTAMP
                )
            )
        }

        verify("project is correctly fetched by ID") {
            val result = repository.getById(id)

            expectThat(result)
                .isEqualTo(
                    Project(
                        id = id,
                        ownerId = OWNER_ID,
                        baseRedirectUrl = BASE_REDIRECT_URL,
                        chainId = CHAIN_ID,
                        customRpcUrl = CUSTOM_RPC_URL,
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentProjectById() {
        verify("null is returned when fetching non-existent project") {
            val result = repository.getById(ProjectId(UUID.randomUUID()))

            expectThat(result)
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyFetchAllProjectsForSomeUser() {
        val id1 = ProjectId(UUID.randomUUID())
        val id2 = ProjectId(UUID.randomUUID())

        suppose("some projects are stored in database") {
            dslContext.batchInsert(
                ProjectRecord(
                    id = id1,
                    ownerId = OWNER_ID,
                    baseRedirectUrl = BASE_REDIRECT_URL,
                    chainId = CHAIN_ID,
                    customRpcUrl = CUSTOM_RPC_URL,
                    createdAt = TestData.TIMESTAMP
                ),
                ProjectRecord(
                    id = id2,
                    ownerId = OWNER_ID,
                    baseRedirectUrl = BASE_REDIRECT_URL,
                    chainId = CHAIN_ID,
                    customRpcUrl = CUSTOM_RPC_URL,
                    createdAt = TestData.TIMESTAMP
                )
            ).execute()
        }

        verify("projects are correctly fetched by user ID") {
            val result = repository.getAllByOwnerId(OWNER_ID)

            expectThat(result.map { it.id })
                .isEqualTo(
                    listOf(id1, id2)
                )
        }
    }

    @Test
    fun mustCorrectlyStoreProject() {
        val id = ProjectId(UUID.randomUUID())
        val project = Project(
            id = id,
            ownerId = OWNER_ID,
            baseRedirectUrl = BASE_REDIRECT_URL,
            chainId = CHAIN_ID,
            customRpcUrl = CUSTOM_RPC_URL,
            createdAt = TestData.TIMESTAMP
        )

        val storedProject = suppose("project is stored in database") {
            repository.store(project)
        }

        verify("storing project returns correct result") {
            expectThat(storedProject)
                .isEqualTo(project)
        }

        verify("project was stored in database") {
            val result = repository.getById(id)

            expectThat(result)
                .isEqualTo(project)
        }
    }
}
