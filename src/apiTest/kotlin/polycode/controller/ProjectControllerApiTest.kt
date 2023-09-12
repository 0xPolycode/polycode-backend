package polycode.controller

import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import polycode.ControllerTestBase
import polycode.TestData
import polycode.config.CustomHeaders
import polycode.exception.ErrorCode
import polycode.features.api.access.model.response.ApiKeyResponse
import polycode.features.api.access.model.response.ProjectResponse
import polycode.features.api.access.model.response.ProjectsResponse
import polycode.features.api.access.model.result.ApiKey
import polycode.features.api.access.model.result.Project
import polycode.features.api.access.model.result.UserWalletAddressIdentifier
import polycode.features.api.access.repository.ApiKeyRepository
import polycode.features.api.access.repository.ProjectRepository
import polycode.features.api.access.repository.UserIdentifierRepository
import polycode.generated.jooq.id.ApiKeyId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import polycode.security.WithMockUser
import polycode.testcontainers.HardhatTestContainer
import polycode.util.BaseUrl
import polycode.util.UtcDateTime
import polycode.util.WalletAddress
import java.time.OffsetDateTime
import java.util.UUID

class ProjectControllerApiTest : ControllerTestBase() {

    @Autowired
    private lateinit var userIdentifierRepository: UserIdentifierRepository

    @Autowired
    private lateinit var projectRepository: ProjectRepository

    @Autowired
    private lateinit var apiKeyRepository: ApiKeyRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)
    }

    @Test
    @WithMockUser
    fun mustCorrectlyCreateProject() {
        val baseRedirectUrl = BaseUrl("base-redirect-url")
        val chainId = TestData.CHAIN_ID
        val customRpcUrl = "custom-rpc-url"

        val response = suppose("request to create project is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/projects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "base_redirect_url": "${baseRedirectUrl.value}",
                                "chain_id": ${chainId.value},
                                "custom_rpc_url": "$customRpcUrl"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ProjectResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    ProjectResponse(
                        id = response.id,
                        ownerId = response.ownerId,
                        baseRedirectUrl = baseRedirectUrl.value,
                        chainId = chainId.value,
                        customRpcUrl = customRpcUrl,
                        createdAt = response.createdAt
                    )
                )
        }

        verify("user is correctly stored into the database") {
            expectThat(userIdentifierRepository.getById(response.ownerId))
                .isEqualTo(
                    UserWalletAddressIdentifier(
                        id = response.ownerId,
                        walletAddress = WalletAddress(HardhatTestContainer.ACCOUNT_ADDRESS_1)
                    )
                )
        }

        verify("project is correctly stored into the database") {
            val storedProject = projectRepository.getById(response.id)!!

            expectThat(storedProject)
                .isEqualTo(
                    Project(
                        id = response.id,
                        ownerId = response.ownerId,
                        baseRedirectUrl = baseRedirectUrl,
                        chainId = chainId,
                        customRpcUrl = customRpcUrl,
                        createdAt = storedProject.createdAt
                    )
                )

            expectThat(storedProject.createdAt.value)
                .isCloseTo(response.createdAt, WITHIN_TIME_TOLERANCE)
            expectThat(storedProject.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    @WithMockUser
    fun mustCorrectlyReturnProjectByApiKey() {
        val userIdentifier = UserWalletAddressIdentifier(
            id = UserId(UUID.randomUUID()),
            walletAddress = WalletAddress(HardhatTestContainer.ACCOUNT_ADDRESS_1)
        )

        suppose("some user identifier exists in database") {
            userIdentifierRepository.store(userIdentifier)
        }

        val project = Project(
            id = ProjectId(UUID.randomUUID()),
            ownerId = userIdentifier.id,
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = "custom-rpc-url",
            createdAt = UtcDateTime(OffsetDateTime.now())
        )

        suppose("some project exists in database") {
            projectRepository.store(project)
        }

        val apiKey = ApiKey(
            id = ApiKeyId(UUID.randomUUID()),
            projectId = project.id,
            apiKey = "api-key",
            createdAt = UtcDateTime(OffsetDateTime.now())
        )

        suppose("some API key exists in database") {
            apiKeyRepository.store(apiKey)
        }

        val response = suppose("request to fetch project by api key is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/projects/by-api-key")
                    .header(CustomHeaders.API_KEY_HEADER, "api-key")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ProjectResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    ProjectResponse(
                        id = project.id,
                        ownerId = project.ownerId,
                        baseRedirectUrl = project.baseRedirectUrl.value,
                        chainId = project.chainId.value,
                        customRpcUrl = project.customRpcUrl,
                        createdAt = response.createdAt
                    )
                )

            expectThat(response.createdAt)
                .isCloseTo(project.createdAt.value, WITHIN_TIME_TOLERANCE)
            expectThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    @WithMockUser
    fun mustCorrectlyReturnProjectById() {
        val userIdentifier = UserWalletAddressIdentifier(
            id = UserId(UUID.randomUUID()),
            walletAddress = WalletAddress(HardhatTestContainer.ACCOUNT_ADDRESS_1)
        )

        suppose("some user identifier exists in database") {
            userIdentifierRepository.store(userIdentifier)
        }

        val project = Project(
            id = ProjectId(UUID.randomUUID()),
            ownerId = userIdentifier.id,
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = "custom-rpc-url",
            createdAt = UtcDateTime(OffsetDateTime.now())
        )

        suppose("some project exists in database") {
            projectRepository.store(project)
        }

        val response = suppose("request to fetch project is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/projects/${project.id.value}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ProjectResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    ProjectResponse(
                        id = project.id,
                        ownerId = project.ownerId,
                        baseRedirectUrl = project.baseRedirectUrl.value,
                        chainId = project.chainId.value,
                        customRpcUrl = project.customRpcUrl,
                        createdAt = response.createdAt
                    )
                )

            expectThat(response.createdAt)
                .isCloseTo(project.createdAt.value, WITHIN_TIME_TOLERANCE)
            expectThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    @WithMockUser
    fun mustReturn404NotFoundForNonExistentProjectId() {
        verify("404 is returned for non-existent project") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/projects/${UUID.randomUUID()}")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    @WithMockUser
    fun mustReturn404NotFoundForNonOwnedProjectId() {
        val userIdentifier = UserWalletAddressIdentifier(
            id = UserId(UUID.randomUUID()),
            walletAddress = WalletAddress("0cafe0babe")
        )

        suppose("some user identifier exists in database") {
            userIdentifierRepository.store(userIdentifier)
        }

        val project = Project(
            id = ProjectId(UUID.randomUUID()),
            ownerId = userIdentifier.id,
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = "custom-rpc-url",
            createdAt = UtcDateTime(OffsetDateTime.now())
        )

        suppose("some project exists in database") {
            projectRepository.store(project)
        }

        verify("404 is returned for non-existent project") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/projects/${project.id.value}")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    @WithMockUser
    fun mustCorrectlyReturnAllProjectsForSomeUser() {
        val userIdentifier = UserWalletAddressIdentifier(
            id = UserId(UUID.randomUUID()),
            walletAddress = WalletAddress(HardhatTestContainer.ACCOUNT_ADDRESS_1)
        )

        suppose("some user identifier exists in database") {
            userIdentifierRepository.store(userIdentifier)
        }

        val project1 = Project(
            id = ProjectId(UUID.randomUUID()),
            ownerId = userIdentifier.id,
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = "custom-rpc-url",
            createdAt = UtcDateTime(OffsetDateTime.now())
        )
        val project2 = Project(
            id = ProjectId(UUID.randomUUID()),
            ownerId = userIdentifier.id,
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = "custom-rpc-url",
            createdAt = UtcDateTime(OffsetDateTime.now())
        )

        suppose("some projects exist in database") {
            projectRepository.store(project1)
            projectRepository.store(project2)
        }

        val response = suppose("request to fetch project is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/projects")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ProjectsResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    ProjectsResponse(
                        listOf(
                            ProjectResponse(
                                id = project1.id,
                                ownerId = project1.ownerId,
                                baseRedirectUrl = project1.baseRedirectUrl.value,
                                chainId = project1.chainId.value,
                                customRpcUrl = project1.customRpcUrl,
                                createdAt = response.projects[0].createdAt
                            ),
                            ProjectResponse(
                                id = project2.id,
                                ownerId = project2.ownerId,
                                baseRedirectUrl = project2.baseRedirectUrl.value,
                                chainId = project2.chainId.value,
                                customRpcUrl = project2.customRpcUrl,
                                createdAt = response.projects[1].createdAt
                            )
                        )
                    )
                )

            expectThat(response.projects[0].createdAt)
                .isCloseTo(project1.createdAt.value, WITHIN_TIME_TOLERANCE)
            expectThat(response.projects[0].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
            expectThat(response.projects[1].createdAt)
                .isCloseTo(project2.createdAt.value, WITHIN_TIME_TOLERANCE)
            expectThat(response.projects[1].createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    @WithMockUser
    fun mustCorrectlyReturnProjectApiKey() {
        val userIdentifier = UserWalletAddressIdentifier(
            id = UserId(UUID.randomUUID()),
            walletAddress = WalletAddress(HardhatTestContainer.ACCOUNT_ADDRESS_1)
        )

        suppose("some user identifier exists in database") {
            userIdentifierRepository.store(userIdentifier)
        }

        val project = Project(
            id = ProjectId(UUID.randomUUID()),
            ownerId = userIdentifier.id,
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = "custom-rpc-url",
            createdAt = UtcDateTime(OffsetDateTime.now())
        )

        suppose("some project exists in database") {
            projectRepository.store(project)
        }

        val apiKey = ApiKey(
            id = ApiKeyId(UUID.randomUUID()),
            projectId = project.id,
            apiKey = "api-key",
            createdAt = UtcDateTime(OffsetDateTime.now())
        )

        suppose("some API key exists in database") {
            apiKeyRepository.store(apiKey)
        }

        val response = suppose("request to fetch API key is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/projects/${project.id.value}/api-key")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ApiKeyResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    ApiKeyResponse(
                        id = apiKey.id,
                        projectId = apiKey.projectId,
                        apiKey = apiKey.apiKey,
                        createdAt = response.createdAt
                    )
                )

            expectThat(response.createdAt)
                .isCloseTo(apiKey.createdAt.value, WITHIN_TIME_TOLERANCE)
            expectThat(response.createdAt)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    @WithMockUser
    fun mustReturn404NotFoundForProjectWithoutApiKey() {
        val userIdentifier = UserWalletAddressIdentifier(
            id = UserId(UUID.randomUUID()),
            walletAddress = WalletAddress(HardhatTestContainer.ACCOUNT_ADDRESS_1)
        )

        suppose("some user identifier exists in database") {
            userIdentifierRepository.store(userIdentifier)
        }

        val project = Project(
            id = ProjectId(UUID.randomUUID()),
            ownerId = userIdentifier.id,
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = "custom-rpc-url",
            createdAt = UtcDateTime(OffsetDateTime.now())
        )

        suppose("some project exists in database") {
            projectRepository.store(project)
        }

        verify("404 is returned for non-existent project") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/projects/${project.id.value}/api-key")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    @WithMockUser
    fun mustCorrectlyCreateApiKeyForSomeProject() {
        val userIdentifier = UserWalletAddressIdentifier(
            id = UserId(UUID.randomUUID()),
            walletAddress = WalletAddress(HardhatTestContainer.ACCOUNT_ADDRESS_1)
        )

        suppose("some user identifier exists in database") {
            userIdentifierRepository.store(userIdentifier)
        }

        val project = Project(
            id = ProjectId(UUID.randomUUID()),
            ownerId = userIdentifier.id,
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = "custom-rpc-url",
            createdAt = UtcDateTime(OffsetDateTime.now())
        )

        suppose("some project exists in database") {
            projectRepository.store(project)
        }

        val response = suppose("request to create API key is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/projects/${project.id.value}/api-key")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ApiKeyResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    ApiKeyResponse(
                        id = response.id,
                        projectId = project.id,
                        apiKey = response.apiKey,
                        createdAt = response.createdAt
                    )
                )
        }

        verify("API key is correctly stored into the database") {
            val apiKey = apiKeyRepository.getById(response.id)!!

            expectThat(apiKey)
                .isEqualTo(
                    ApiKey(
                        id = response.id,
                        projectId = response.projectId,
                        apiKey = response.apiKey,
                        createdAt = apiKey.createdAt
                    )
                )

            expectThat(apiKey.createdAt.value)
                .isCloseTo(response.createdAt, WITHIN_TIME_TOLERANCE)
            expectThat(apiKey.createdAt.value)
                .isCloseToUtcNow(WITHIN_TIME_TOLERANCE)
        }
    }

    @Test
    @WithMockUser
    fun mustReturn400BadRequestWhenCreatingAnotherApiKeyForSomeProject() {
        val userIdentifier = UserWalletAddressIdentifier(
            id = UserId(UUID.randomUUID()),
            walletAddress = WalletAddress(HardhatTestContainer.ACCOUNT_ADDRESS_1)
        )

        suppose("some user identifier exists in database") {
            userIdentifierRepository.store(userIdentifier)
        }

        val project = Project(
            id = ProjectId(UUID.randomUUID()),
            ownerId = userIdentifier.id,
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = "custom-rpc-url",
            createdAt = UtcDateTime(OffsetDateTime.now())
        )

        suppose("some project exists in database") {
            projectRepository.store(project)
        }

        val apiKey = ApiKey(
            id = ApiKeyId(UUID.randomUUID()),
            projectId = project.id,
            apiKey = "api-key",
            createdAt = UtcDateTime(OffsetDateTime.now())
        )

        suppose("some API key exists in database") {
            apiKeyRepository.store(apiKey)
        }

        verify("400 is returned for project which already has API key") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/projects/${project.id.value}/api-key")
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.API_KEY_ALREADY_EXISTS)
        }
    }

    @Test
    @WithMockUser(address = HardhatTestContainer.ACCOUNT_ADDRESS_2)
    fun mustReturn404NotFoundWhenCreatingApiKeyByNonProjectOwner() {
        val userIdentifier = UserWalletAddressIdentifier(
            id = UserId(UUID.randomUUID()),
            walletAddress = WalletAddress(HardhatTestContainer.ACCOUNT_ADDRESS_1)
        )

        suppose("some user identifier exists in database") {
            userIdentifierRepository.store(userIdentifier)
        }

        val project = Project(
            id = ProjectId(UUID.randomUUID()),
            ownerId = userIdentifier.id,
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = "custom-rpc-url",
            createdAt = UtcDateTime(OffsetDateTime.now())
        )

        suppose("some project exists in database") {
            projectRepository.store(project)
        }

        verify("404 is returned for non-owned project") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/projects/${project.id.value}/api-key")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }
}
