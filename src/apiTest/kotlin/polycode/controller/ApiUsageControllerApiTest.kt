package polycode.controller

import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import polycode.ControllerTestBase
import polycode.TestData
import polycode.config.ApiRateProperties
import polycode.config.CustomHeaders
import polycode.exception.ErrorCode
import polycode.features.api.access.model.result.Project
import polycode.features.api.usage.model.response.ApiUsagePeriodResponse
import polycode.features.api.usage.model.result.RequestUsage
import polycode.generated.jooq.enums.UserIdentifierType
import polycode.generated.jooq.id.ApiKeyId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import polycode.generated.jooq.tables.records.ApiKeyRecord
import polycode.generated.jooq.tables.records.ProjectRecord
import polycode.generated.jooq.tables.records.UserIdentifierRecord
import polycode.security.WithMockUser
import polycode.testcontainers.HardhatTestContainer
import polycode.util.BaseUrl
import polycode.util.WalletAddress
import java.util.UUID

class ApiUsageControllerApiTest : ControllerTestBase() {

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
    }

    @Autowired
    private lateinit var apiRateProperties: ApiRateProperties

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)

        dslContext.executeInsert(
            UserIdentifierRecord(
                id = OWNER_ID,
                userIdentifier = WalletAddress(HardhatTestContainer.ACCOUNT_ADDRESS_1).rawValue,
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
    @WithMockUser
    fun mustCorrectlyFetchApiUsageForUser() {
        val response = suppose("request to API usage for user is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/api-usage")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ApiUsagePeriodResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    ApiUsagePeriodResponse(
                        userId = OWNER_ID,
                        writeRequestUsage = RequestUsage(
                            used = 0,
                            remaining = apiRateProperties.freeTierWriteRequests
                        ),
                        readRequestUsage = RequestUsage(
                            used = 0,
                            remaining = apiRateProperties.freeTierReadRequests
                        ),
                        startDate = response.startDate,
                        endDate = response.endDate
                    )
                )
        }
    }

    @Test
    fun mustReturn401UnauthorizedWhenFetchingApiUsageForUserWithoutJwt() {
        verify("401 is returned for missing JWT") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/api-usage")
            )
                .andExpect(MockMvcResultMatchers.status().isUnauthorized)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.BAD_AUTHENTICATION)
        }
    }

    @Test
    fun mustCorrectlyFetchApiUsageForApiKey() {
        val response = suppose("request to API usage for API key is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/api-usage/by-api-key")
                    .header(CustomHeaders.API_KEY_HEADER, API_KEY)
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ApiUsagePeriodResponse::class.java)
        }

        verify("correct response is returned") {
            expectThat(response)
                .isEqualTo(
                    ApiUsagePeriodResponse(
                        userId = OWNER_ID,
                        writeRequestUsage = RequestUsage(
                            used = 0,
                            remaining = apiRateProperties.freeTierWriteRequests
                        ),
                        readRequestUsage = RequestUsage(
                            used = 0,
                            remaining = apiRateProperties.freeTierReadRequests
                        ),
                        startDate = response.startDate,
                        endDate = response.endDate
                    )
                )
        }
    }

    @Test
    fun mustReturn401UnauthorizedWhenFetchingApiUsageForInvalidApiKey() {
        verify("401 is returned for invalid API key") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/api-usage/by-api-key")
                    .header(CustomHeaders.API_KEY_HEADER, "invalid-api-key")
            )
                .andExpect(MockMvcResultMatchers.status().isUnauthorized)
                .andReturn()

            expectResponseErrorCode(response, ErrorCode.NON_EXISTENT_API_KEY)
        }
    }
}
