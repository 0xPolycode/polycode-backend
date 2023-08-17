package polycode.controller

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockHttpServletRequest
import polycode.JsonSchemaDocumentation
import polycode.TestBase
import polycode.TestData
import polycode.exception.ApiKeyAlreadyExistsException
import polycode.exception.ResourceNotFoundException
import polycode.features.api.access.controller.ProjectController
import polycode.features.api.access.model.params.CreateProjectParams
import polycode.features.api.access.model.request.CreateProjectRequest
import polycode.features.api.access.model.response.ApiKeyResponse
import polycode.features.api.access.model.response.ProjectResponse
import polycode.features.api.access.model.response.ProjectsResponse
import polycode.features.api.access.model.result.ApiKey
import polycode.features.api.access.model.result.Project
import polycode.features.api.access.model.result.UserWalletAddressIdentifier
import polycode.features.api.access.service.ProjectService
import polycode.features.api.analytics.service.AnalyticsService
import polycode.generated.jooq.id.ApiKeyId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import polycode.util.BaseUrl
import polycode.util.UtcDateTime
import polycode.util.WalletAddress
import java.time.OffsetDateTime
import java.util.UUID

class ProjectControllerTest : TestBase() {

    companion object {
        private val CREATED_AT = UtcDateTime(OffsetDateTime.parse("2022-01-01T00:00:00Z"))
    }

    @Test
    fun mustCorrectlyCreateProject() {
        val params = CreateProjectParams(
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = "custom-rpc-url"
        )
        val result = Project(
            id = ProjectId(UUID.randomUUID()),
            ownerId = UserId(UUID.randomUUID()),
            baseRedirectUrl = params.baseRedirectUrl,
            chainId = params.chainId,
            customRpcUrl = params.customRpcUrl,
            createdAt = CREATED_AT
        )
        val userIdentifier = UserWalletAddressIdentifier(
            id = result.ownerId,
            walletAddress = WalletAddress("a")
        )

        val service = mock<ProjectService>()
        val analyticsService = mock<AnalyticsService>()

        suppose("project will be created") {
            call(service.createProject(userIdentifier, params))
                .willReturn(result)
        }

        val controller = ProjectController(service, analyticsService)

        verify("controller returns correct response") {
            val request = CreateProjectRequest(
                baseRedirectUrl = params.baseRedirectUrl.value,
                chainId = params.chainId.value,
                customRpcUrl = params.customRpcUrl
            )
            val response = controller.createProject(userIdentifier, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(ProjectResponse(result)))
        }
    }

    @Test
    fun mustCorrectlyGetProjectById() {
        val result = Project(
            id = ProjectId(UUID.randomUUID()),
            ownerId = UserId(UUID.randomUUID()),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = "custom-rpc-url",
            createdAt = CREATED_AT
        )
        val userIdentifier = UserWalletAddressIdentifier(
            id = result.ownerId,
            walletAddress = WalletAddress("a")
        )

        val service = mock<ProjectService>()
        val analyticsService = mock<AnalyticsService>()

        suppose("project will be returned") {
            call(service.getProjectById(userIdentifier, result.id))
                .willReturn(result)
        }

        val controller = ProjectController(service, analyticsService)

        verify("controller returns correct response") {
            val response = controller.getById(userIdentifier, result.id)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(ProjectResponse(result)))
        }
    }

    @Test
    fun mustCorrectlyGetAllProjectsForUser() {
        val userIdentifier = UserWalletAddressIdentifier(
            id = UserId(UUID.randomUUID()),
            walletAddress = WalletAddress("a")
        )
        val result = listOf(
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

        val service = mock<ProjectService>()
        val analyticsService = mock<AnalyticsService>()

        suppose("projects will be returned") {
            call(service.getAllProjectsForUser(userIdentifier))
                .willReturn(result)
        }

        val controller = ProjectController(service, analyticsService)

        verify("controller returns correct response") {
            val response = controller.getAll(userIdentifier)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(ProjectsResponse(result.map { ProjectResponse(it) })))
        }
    }

    @Test
    fun mustCorrectlyGetApiKey() {
        val result = ApiKey(
            id = ApiKeyId(UUID.randomUUID()),
            projectId = ProjectId(UUID.randomUUID()),
            apiKey = "api-key",
            createdAt = CREATED_AT
        )
        val userIdentifier = UserWalletAddressIdentifier(
            id = UserId(UUID.randomUUID()),
            walletAddress = WalletAddress("a")
        )

        val service = mock<ProjectService>()
        val analyticsService = mock<AnalyticsService>()

        suppose("API key will be returned") {
            call(service.getProjectApiKeys(userIdentifier, result.projectId))
                .willReturn(listOf(result))
        }

        val controller = ProjectController(service, analyticsService)

        verify("controller returns correct response") {
            val response = controller.getApiKey(userIdentifier, result.projectId)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(ApiKeyResponse(result)))
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionForNonExistentApiKey() {
        val projectId = ProjectId(UUID.randomUUID())
        val userIdentifier = UserWalletAddressIdentifier(
            id = UserId(UUID.randomUUID()),
            walletAddress = WalletAddress("a")
        )

        val service = mock<ProjectService>()
        val analyticsService = mock<AnalyticsService>()

        suppose("empty list will be returned") {
            call(service.getProjectApiKeys(userIdentifier, projectId))
                .willReturn(listOf())
        }

        val controller = ProjectController(service, analyticsService)

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                controller.getApiKey(userIdentifier, projectId)
            }
        }
    }

    @Test
    fun mustCorrectlyCreateApiKey() {
        val result = ApiKey(
            id = ApiKeyId(UUID.randomUUID()),
            projectId = ProjectId(UUID.randomUUID()),
            apiKey = "api-key",
            createdAt = CREATED_AT
        )
        val userIdentifier = UserWalletAddressIdentifier(
            id = UserId(UUID.randomUUID()),
            walletAddress = WalletAddress("a")
        )

        val service = mock<ProjectService>()
        val analyticsService = mock<AnalyticsService>()

        suppose("no API keys exist") {
            call(service.getProjectApiKeys(userIdentifier, result.projectId))
                .willReturn(emptyList())
        }

        suppose("API key will be created") {
            call(service.createApiKey(userIdentifier, result.projectId))
                .willReturn(result)
        }

        val controller = ProjectController(service, analyticsService)

        verify("controller returns correct response") {
            val response = controller.createApiKey(userIdentifier, result.projectId, MockHttpServletRequest())

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(ApiKeyResponse(result)))
        }
    }

    @Test
    fun mustThrowApiKeyAlreadyExistsExceptionWhenApiKeyAlreadyExists() {
        val result = ApiKey(
            id = ApiKeyId(UUID.randomUUID()),
            projectId = ProjectId(UUID.randomUUID()),
            apiKey = "api-key",
            createdAt = CREATED_AT
        )
        val userIdentifier = UserWalletAddressIdentifier(
            id = UserId(UUID.randomUUID()),
            walletAddress = WalletAddress("a")
        )

        val service = mock<ProjectService>()
        val analyticsService = mock<AnalyticsService>()

        suppose("single API keys exist") {
            call(service.getProjectApiKeys(userIdentifier, result.projectId))
                .willReturn(listOf(result))
        }

        val controller = ProjectController(service, analyticsService)

        verify("ApiKeyAlreadyExistsException is thrown") {
            expectThrows<ApiKeyAlreadyExistsException> {
                controller.createApiKey(userIdentifier, result.projectId, MockHttpServletRequest())
            }
        }
    }
}
