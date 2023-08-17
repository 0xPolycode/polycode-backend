package polycode.service

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import polycode.TestBase
import polycode.TestData
import polycode.exception.ResourceNotFoundException
import polycode.features.api.access.model.params.CreateProjectParams
import polycode.features.api.access.model.result.ApiKey
import polycode.features.api.access.model.result.Project
import polycode.features.api.access.model.result.UserWalletAddressIdentifier
import polycode.features.api.access.repository.ApiKeyRepository
import polycode.features.api.access.repository.ProjectRepository
import polycode.features.api.access.service.ProjectServiceImpl
import polycode.generated.jooq.id.ApiKeyId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import polycode.util.BaseUrl
import polycode.util.UtcDateTime
import polycode.util.WalletAddress
import java.time.OffsetDateTime
import java.util.UUID

class ProjectServiceTest : TestBase() {

    companion object {
        private val CREATED_AT = UtcDateTime(OffsetDateTime.parse("2022-01-01T00:00:00Z"))
        private const val API_KEY_BYTES = 33
    }

    @Test
    fun mustCorrectlyCreateProject() {
        val uuid = ProjectId(UUID.randomUUID())
        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be returned") {
            call(uuidProvider.getUuid(ProjectId))
                .willReturn(uuid)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some UTC date-time will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(CREATED_AT)
        }

        val params = CreateProjectParams(
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = "custom-rpc-url"
        )
        val userIdentifier = UserWalletAddressIdentifier(
            id = UserId(UUID.randomUUID()),
            walletAddress = WalletAddress("b")
        )
        val project = Project(
            id = uuid,
            ownerId = userIdentifier.id,
            baseRedirectUrl = params.baseRedirectUrl,
            chainId = params.chainId,
            customRpcUrl = params.customRpcUrl,
            createdAt = utcDateTimeProvider.getUtcDateTime()
        )
        val projectRepository = mock<ProjectRepository>()

        suppose("project will be stored into the database") {
            call(projectRepository.store(project))
                .willReturn(project)
        }

        val service = ProjectServiceImpl(
            uuidProvider = uuidProvider,
            utcDateTimeProvider = utcDateTimeProvider,
            randomProvider = mock(),
            projectRepository = projectRepository,
            apiKeyRepository = mock()
        )

        verify("project is correctly stored into the database") {
            expectThat(service.createProject(userIdentifier, params))
                .isEqualTo(project)
        }
    }

    @Test
    fun mustCorrectlyFetchProjectById() {
        val project = Project(
            id = ProjectId(UUID.randomUUID()),
            ownerId = UserId(UUID.randomUUID()),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = "custom-rpc-url",
            createdAt = CREATED_AT
        )
        val projectRepository = mock<ProjectRepository>()

        suppose("project will be returned by ID") {
            call(projectRepository.getById(project.id))
                .willReturn(project)
        }

        val userIdentifier = UserWalletAddressIdentifier(
            id = project.ownerId,
            walletAddress = WalletAddress("b")
        )

        val service = ProjectServiceImpl(
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            randomProvider = mock(),
            projectRepository = projectRepository,
            apiKeyRepository = mock()
        )

        verify("project is fetched from database by ID") {
            expectThat(service.getProjectById(userIdentifier, project.id))
                .isEqualTo(project)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenFetchingNonExistentProjectById() {
        val projectRepository = mock<ProjectRepository>()

        suppose("null will be returned for any project ID") {
            call(projectRepository.getById(anyValueClass(ProjectId(UUID.randomUUID()))))
                .willReturn(null)
        }

        val userIdentifier = UserWalletAddressIdentifier(
            id = UserId(UUID.randomUUID()),
            walletAddress = WalletAddress("b")
        )

        val service = ProjectServiceImpl(
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            randomProvider = mock(),
            projectRepository = projectRepository,
            apiKeyRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.getProjectById(userIdentifier, ProjectId(UUID.randomUUID()))
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionUserIsFetchingNonOwnedProjectById() {
        val project = Project(
            id = ProjectId(UUID.randomUUID()),
            ownerId = UserId(UUID.randomUUID()),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = "custom-rpc-url",
            createdAt = CREATED_AT
        )
        val projectRepository = mock<ProjectRepository>()

        suppose("project will be returned by ID") {
            call(projectRepository.getById(project.id))
                .willReturn(project)
        }

        val userIdentifier = UserWalletAddressIdentifier(
            id = UserId(UUID.randomUUID()),
            walletAddress = WalletAddress("b")
        )

        val service = ProjectServiceImpl(
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            randomProvider = mock(),
            projectRepository = projectRepository,
            apiKeyRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.getProjectById(userIdentifier, project.id)
            }
        }
    }

    @Test
    fun mustCorrectlyFetchAllProjectsForUser() {
        val userIdentifier = UserWalletAddressIdentifier(
            id = UserId(UUID.randomUUID()),
            walletAddress = WalletAddress("b")
        )
        val projects = listOf(
            Project(
                id = ProjectId(UUID.randomUUID()),
                ownerId = userIdentifier.id,
                baseRedirectUrl = BaseUrl("base-redirect-url-1"),
                chainId = TestData.CHAIN_ID,
                customRpcUrl = "custom-rpc-url-1",
                createdAt = CREATED_AT
            ),
            Project(
                id = ProjectId(UUID.randomUUID()),
                ownerId = userIdentifier.id,
                baseRedirectUrl = BaseUrl("base-redirect-url-2"),
                chainId = TestData.CHAIN_ID,
                customRpcUrl = "custom-rpc-url-2",
                createdAt = CREATED_AT
            )
        )
        val projectRepository = mock<ProjectRepository>()

        suppose("project will be returned for user") {
            call(projectRepository.getAllByOwnerId(userIdentifier.id))
                .willReturn(projects)
        }

        val service = ProjectServiceImpl(
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            randomProvider = mock(),
            projectRepository = projectRepository,
            apiKeyRepository = mock()
        )

        verify("project is fetched from database for some user") {
            expectThat(service.getAllProjectsForUser(userIdentifier))
                .isEqualTo(projects)
        }
    }

    @Test
    fun mustCorrectlyFetchProjectApiKeys() {
        val project = Project(
            id = ProjectId(UUID.randomUUID()),
            ownerId = UserId(UUID.randomUUID()),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = "custom-rpc-url",
            createdAt = CREATED_AT
        )
        val projectRepository = mock<ProjectRepository>()

        suppose("project will be returned by ID") {
            call(projectRepository.getById(project.id))
                .willReturn(project)
        }

        val userIdentifier = UserWalletAddressIdentifier(
            id = project.ownerId,
            walletAddress = WalletAddress("b")
        )

        val apiKeys = listOf(
            ApiKey(
                id = ApiKeyId(UUID.randomUUID()),
                projectId = project.id,
                apiKey = "api-key-1",
                createdAt = CREATED_AT
            ),
            ApiKey(
                id = ApiKeyId(UUID.randomUUID()),
                projectId = project.id,
                apiKey = "api-key-2",
                createdAt = CREATED_AT
            )
        )

        val apiKeyRepository = mock<ApiKeyRepository>()

        suppose("some API keys will be returned by project ID") {
            call(apiKeyRepository.getAllByProjectId(project.id))
                .willReturn(apiKeys)
        }

        val service = ProjectServiceImpl(
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            randomProvider = mock(),
            projectRepository = projectRepository,
            apiKeyRepository = apiKeyRepository
        )

        verify("project is fetched from database by ID") {
            expectThat(service.getProjectApiKeys(userIdentifier, project.id))
                .isEqualTo(apiKeys)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenUserIsFetchingProjectApiKeysForNonOwnedProject() {
        val project = Project(
            id = ProjectId(UUID.randomUUID()),
            ownerId = UserId(UUID.randomUUID()),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = "custom-rpc-url",
            createdAt = CREATED_AT
        )
        val projectRepository = mock<ProjectRepository>()

        suppose("project will be returned by ID") {
            call(projectRepository.getById(project.id))
                .willReturn(project)
        }

        val userIdentifier = UserWalletAddressIdentifier(
            id = UserId(UUID.randomUUID()),
            walletAddress = WalletAddress("b")
        )

        val service = ProjectServiceImpl(
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            randomProvider = mock(),
            projectRepository = projectRepository,
            apiKeyRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.getProjectApiKeys(userIdentifier, project.id)
            }
        }
    }

    @Test
    fun mustCorrectlyCreateApiKey() {
        val project = Project(
            id = ProjectId(UUID.randomUUID()),
            ownerId = UserId(UUID.randomUUID()),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = "custom-rpc-url",
            createdAt = CREATED_AT
        )
        val projectRepository = mock<ProjectRepository>()

        suppose("project will be returned by ID") {
            call(projectRepository.getById(project.id))
                .willReturn(project)
        }

        val randomProvider = mock<RandomProvider>()

        suppose("some random bytes will be returned") {
            call(randomProvider.getBytes(API_KEY_BYTES))
                .willReturn(ByteArray(API_KEY_BYTES))
        }

        val uuid = ApiKeyId(UUID.randomUUID())
        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be returned") {
            call(uuidProvider.getUuid(ApiKeyId))
                .willReturn(uuid)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some UTC date-time will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(CREATED_AT)
        }

        val apiKey = ApiKey(
            id = uuid,
            projectId = project.id,
            apiKey = "AAAAA.AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
            createdAt = CREATED_AT
        )
        val apiKeyRepository = mock<ApiKeyRepository>()

        suppose("API key will be stored into the database") {
            call(apiKeyRepository.store(apiKey))
                .willReturn(apiKey)
        }

        val userIdentifier = UserWalletAddressIdentifier(
            id = project.ownerId,
            walletAddress = WalletAddress("b")
        )

        val service = ProjectServiceImpl(
            uuidProvider = uuidProvider,
            utcDateTimeProvider = utcDateTimeProvider,
            randomProvider = randomProvider,
            projectRepository = projectRepository,
            apiKeyRepository = apiKeyRepository
        )

        verify("API key is correctly stored into the database") {
            expectThat(service.createApiKey(userIdentifier, project.id))
                .isEqualTo(apiKey)
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenUserCreatesApiKeyForNonOwnedProject() {
        val project = Project(
            id = ProjectId(UUID.randomUUID()),
            ownerId = UserId(UUID.randomUUID()),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = "custom-rpc-url",
            createdAt = CREATED_AT
        )
        val projectRepository = mock<ProjectRepository>()

        suppose("project will be returned by ID") {
            call(projectRepository.getById(project.id))
                .willReturn(project)
        }

        val userIdentifier = UserWalletAddressIdentifier(
            id = UserId(UUID.randomUUID()),
            walletAddress = WalletAddress("b")
        )

        val service = ProjectServiceImpl(
            uuidProvider = mock(),
            utcDateTimeProvider = mock(),
            randomProvider = mock(),
            projectRepository = projectRepository,
            apiKeyRepository = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.createApiKey(userIdentifier, project.id)
            }
        }
    }
}
