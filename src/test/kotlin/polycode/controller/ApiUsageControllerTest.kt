package polycode.controller

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import polycode.JsonSchemaDocumentation
import polycode.TestBase
import polycode.TestData
import polycode.features.api.access.model.result.Project
import polycode.features.api.access.model.result.UserWalletAddressIdentifier
import polycode.features.api.usage.controller.ApiUsageController
import polycode.features.api.usage.model.response.ApiUsagePeriodResponse
import polycode.features.api.usage.model.result.ApiUsagePeriod
import polycode.features.api.usage.model.result.RequestUsage
import polycode.features.api.usage.repository.ApiRateLimitRepository
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import polycode.service.UtcDateTimeProvider
import polycode.util.BaseUrl
import polycode.util.WalletAddress
import java.util.UUID
import kotlin.time.Duration.Companion.days

class ApiUsageControllerTest : TestBase() {

    companion object {
        private val USER_IDENTIFIER = UserWalletAddressIdentifier(
            id = UserId(UUID.randomUUID()),
            walletAddress = WalletAddress("a")
        )
        private val PROJECT = Project(
            id = ProjectId(UUID.randomUUID()),
            ownerId = USER_IDENTIFIER.id,
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = TestData.CHAIN_ID,
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
        private val API_USAGE_PERIOD = ApiUsagePeriod(
            userId = USER_IDENTIFIER.id,
            writeRequestUsage = RequestUsage(0, 0),
            readRequestUsage = RequestUsage(1, 1),
            startDate = TestData.TIMESTAMP,
            endDate = TestData.TIMESTAMP + 30.days
        )
        private val RESPONSE = ResponseEntity.ok(
            ApiUsagePeriodResponse(
                userId = USER_IDENTIFIER.id,
                writeRequestUsage = RequestUsage(0, 0),
                readRequestUsage = RequestUsage(1, 1),
                startDate = TestData.TIMESTAMP.value,
                endDate = (TestData.TIMESTAMP + 30.days).value
            )
        )
    }

    @Test
    fun mustCorrectlyGetCurrentApiUsageInfoForUser() {
        val apiRateLimitRepository = mock<ApiRateLimitRepository>()

        suppose("some api usage will be returned") {
            call(apiRateLimitRepository.getCurrentApiUsagePeriod(USER_IDENTIFIER.id, TestData.TIMESTAMP))
                .willReturn(API_USAGE_PERIOD)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val controller = ApiUsageController(apiRateLimitRepository, utcDateTimeProvider)

        verify("controller returns correct response") {
            val response = controller.getCurrentApiUsageInfoForUser(USER_IDENTIFIER)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(RESPONSE)
        }
    }

    @Test
    fun mustCorrectlyGetCurrentApiUsageInfoForApiKey() {
        val apiRateLimitRepository = mock<ApiRateLimitRepository>()

        suppose("some api usage will be returned") {
            call(apiRateLimitRepository.getCurrentApiUsagePeriod(USER_IDENTIFIER.id, TestData.TIMESTAMP))
                .willReturn(API_USAGE_PERIOD)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val controller = ApiUsageController(apiRateLimitRepository, utcDateTimeProvider)

        verify("controller returns correct response") {
            val response = controller.getCurrentApiUsageInfoForApiKey(PROJECT)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(RESPONSE)
        }
    }
}
