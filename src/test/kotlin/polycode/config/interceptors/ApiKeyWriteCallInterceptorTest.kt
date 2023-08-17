package polycode.config.interceptors

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerMapping
import polycode.TestBase
import polycode.TestData
import polycode.config.CustomHeaders
import polycode.config.JsonConfig
import polycode.config.interceptors.annotation.ApiWriteLimitedMapping
import polycode.config.interceptors.annotation.IdType
import polycode.exception.ErrorCode
import polycode.exception.ErrorResponse
import polycode.features.api.access.model.result.ApiKey
import polycode.features.api.access.repository.ApiKeyRepository
import polycode.features.api.usage.repository.ApiRateLimitRepository
import polycode.features.api.usage.repository.UserIdResolverRepository
import polycode.generated.jooq.id.ApiKeyId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import polycode.service.UtcDateTimeProvider
import java.io.PrintWriter
import java.util.UUID
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class ApiKeyWriteCallInterceptorTest : TestBase() {

    companion object {
        // @formatter:off
        @Suppress("unused")
        fun nonAnnotated() {}
        @ApiWriteLimitedMapping(IdType.PROJECT_ID, RequestMethod.POST, "/test-path")
        @Suppress("unused")
        fun projectIdAnnotated() {}
        @ApiWriteLimitedMapping(IdType.ASSET_SEND_REQUEST_ID, RequestMethod.POST, "/test-path/{id}")
        @Suppress("unused", "UNUSED_PARAMETER")
        fun otherIdAnnotated(@PathVariable id: String) {}
        // @formatter:on

        private val OBJECT_MAPPER = JsonConfig().objectMapper()
        private val PATH_VARIABLES = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE
    }

    @Test
    fun mustNotHandleNonAnnotatedMethod() {
        val apiKeyRepository = mock<ApiKeyRepository>()
        val apiRateLimitRepository = mock<ApiRateLimitRepository>()
        val userIdResolverRepository = mock<UserIdResolverRepository>()
        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        val handler = HandlerMethod(Companion, Companion::class.java.methods.find { it.name == "nonAnnotated" }!!)
        val interceptor = ApiKeyWriteCallInterceptor(
            apiKeyRepository = apiKeyRepository,
            apiRateLimitRepository = apiRateLimitRepository,
            userIdResolverRepository = userIdResolverRepository,
            utcDateTimeProvider = utcDateTimeProvider,
            objectMapper = OBJECT_MAPPER
        )

        val request = mock<HttpServletRequest>()
        val response = mock<HttpServletResponse>()

        verify("unannotated method is not handled") {
            interceptor.preHandle(request, response, handler)
            interceptor.afterCompletion(request, response, handler, null)

            expectNoInteractions(
                apiKeyRepository,
                apiRateLimitRepository,
                userIdResolverRepository,
                utcDateTimeProvider,
                request,
                response
            )
        }
    }

    @Test
    fun mustCorrectlyHandleAnnotatedMethodWhenThereIsSomeRemainingWriteLimitAndReturnStatusIsSuccessForApiKey() {
        val apiKey = "api-key"
        val request = mock<HttpServletRequest>()

        suppose("request will return API key header") {
            call(request.getHeader(CustomHeaders.API_KEY_HEADER))
                .willReturn(apiKey)
        }

        val projectId = ProjectId(UUID.randomUUID())
        val apiKeyRepository = mock<ApiKeyRepository>()

        suppose("API key repository will return some API key") {
            call(apiKeyRepository.getByValue(apiKey))
                .willReturn(
                    ApiKey(
                        id = ApiKeyId(UUID.randomUUID()),
                        projectId = projectId,
                        apiKey = apiKey,
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }

        val userIdResolverRepository = mock<UserIdResolverRepository>()
        val userId = UserId(UUID.randomUUID())

        suppose("userId will be resolved in the repository") {
            call(userIdResolverRepository.getByProjectId(projectId))
                .willReturn(userId)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some date-time will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val apiRateLimitRepository = mock<ApiRateLimitRepository>()

        suppose("remaining read API rate limit will not be zero") {
            call(apiRateLimitRepository.remainingWriteLimit(userId, TestData.TIMESTAMP))
                .willReturn(1)
        }

        val response = mock<HttpServletResponse>()

        suppose("response will have successful status") {
            call(response.status)
                .willReturn(HttpStatus.OK.value())
        }

        val handler = HandlerMethod(Companion, Companion::class.java.methods.find { it.name == "projectIdAnnotated" }!!)
        val interceptor = ApiKeyWriteCallInterceptor(
            apiKeyRepository = apiKeyRepository,
            apiRateLimitRepository = apiRateLimitRepository,
            userIdResolverRepository = userIdResolverRepository,
            utcDateTimeProvider = utcDateTimeProvider,
            objectMapper = OBJECT_MAPPER
        )

        verify("annotated method is correctly handled") {
            val handleResult = interceptor.preHandle(request, response, handler)

            expectThat(handleResult)
                .isTrue()

            interceptor.afterCompletion(request, response, handler, null)

            expectInteractions(apiKeyRepository) {
                twice.getByValue(apiKey)
            }

            expectInteractions(apiRateLimitRepository) {
                once.remainingWriteLimit(userId, TestData.TIMESTAMP)
                once.addWriteCall(userId, TestData.TIMESTAMP, RequestMethod.POST, "/test-path")
            }

            expectInteractions(userIdResolverRepository) {
                twice.getByProjectId(projectId)
            }
        }
    }

    @Test
    fun mustCorrectlyHandleAnnotatedMethodWhenThereIsSomeRemainingWriteLimitAndReturnStatusIsSuccessForPathId() {
        val projectId = UUID.randomUUID()
        val idType = IdType.ASSET_SEND_REQUEST_ID
        val request = mock<HttpServletRequest>()

        suppose("request will contain id") {
            call(request.getAttribute(PATH_VARIABLES))
                .willReturn(mapOf(idType.idVariableName to projectId.toString()))
        }

        val userIdResolverRepository = mock<UserIdResolverRepository>()
        val userId = UserId(UUID.randomUUID())

        suppose("userId will be resolved in the repository") {
            call(userIdResolverRepository.getUserId(idType, projectId))
                .willReturn(userId)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some date-time will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val apiRateLimitRepository = mock<ApiRateLimitRepository>()

        suppose("remaining read API rate limit will not be zero") {
            call(apiRateLimitRepository.remainingWriteLimit(userId, TestData.TIMESTAMP))
                .willReturn(1)
        }

        val response = mock<HttpServletResponse>()

        suppose("response will have successful status") {
            call(response.status)
                .willReturn(HttpStatus.OK.value())
        }

        val apiKeyRepository = mock<ApiKeyRepository>()

        val handler = HandlerMethod(Companion, Companion::class.java.methods.find { it.name == "otherIdAnnotated" }!!)
        val interceptor = ApiKeyWriteCallInterceptor(
            apiKeyRepository = apiKeyRepository,
            apiRateLimitRepository = apiRateLimitRepository,
            userIdResolverRepository = userIdResolverRepository,
            utcDateTimeProvider = utcDateTimeProvider,
            objectMapper = OBJECT_MAPPER
        )

        verify("annotated method is correctly handled") {
            val handleResult = interceptor.preHandle(request, response, handler)

            expectThat(handleResult)
                .isTrue()

            interceptor.afterCompletion(request, response, handler, null)

            expectNoInteractions(apiKeyRepository)

            expectInteractions(apiRateLimitRepository) {
                once.remainingWriteLimit(userId, TestData.TIMESTAMP)
                once.addWriteCall(userId, TestData.TIMESTAMP, RequestMethod.POST, "/test-path/{id}")
            }

            expectInteractions(userIdResolverRepository) {
                twice.getUserId(idType, projectId)
            }
        }
    }

    @Test
    fun mustCorrectlyHandleAnnotatedMethodWhenThereIsNoRemainingWriteLimitAndReturnStatusIsNonSuccess() {
        val projectId = UUID.randomUUID()
        val idType = IdType.ASSET_SEND_REQUEST_ID
        val request = mock<HttpServletRequest>()

        suppose("request will contain id") {
            call(request.getAttribute(PATH_VARIABLES))
                .willReturn(mapOf(idType.idVariableName to projectId.toString()))
        }

        val userIdResolverRepository = mock<UserIdResolverRepository>()
        val userId = UserId(UUID.randomUUID())

        suppose("userId will be resolved in the repository") {
            call(userIdResolverRepository.getUserId(idType, projectId))
                .willReturn(userId)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some date-time will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(TestData.TIMESTAMP)
        }

        val apiRateLimitRepository = mock<ApiRateLimitRepository>()

        suppose("remaining read API rate limit will be zero") {
            call(apiRateLimitRepository.remainingWriteLimit(userId, TestData.TIMESTAMP))
                .willReturn(0)
        }

        val response = mock<HttpServletResponse>()

        suppose("response will have bad request status") {
            call(response.status)
                .willReturn(HttpStatus.BAD_REQUEST.value())
        }

        val writer = mock<PrintWriter>()

        suppose("response will return a writer") {
            call(response.writer)
                .willReturn(writer)
        }

        val apiKeyRepository = mock<ApiKeyRepository>()

        val handler = HandlerMethod(Companion, Companion::class.java.methods.find { it.name == "otherIdAnnotated" }!!)
        val interceptor = ApiKeyWriteCallInterceptor(
            apiKeyRepository = apiKeyRepository,
            apiRateLimitRepository = apiRateLimitRepository,
            userIdResolverRepository = userIdResolverRepository,
            utcDateTimeProvider = utcDateTimeProvider,
            objectMapper = OBJECT_MAPPER
        )

        verify("annotated method is correctly handled") {
            val handleResult = interceptor.preHandle(request, response, handler)

            expectThat(handleResult)
                .isFalse()

            interceptor.afterCompletion(request, response, handler, null)

            expectNoInteractions(apiKeyRepository)

            expectInteractions(apiRateLimitRepository) {
                once.remainingWriteLimit(userId, TestData.TIMESTAMP)
            }

            expectInteractions(userIdResolverRepository) {
                twice.getUserId(idType, projectId)
            }

            expectInteractions(response) {
                once.status = HttpStatus.PAYMENT_REQUIRED.value()
                once.writer
                once.status
            }

            expectInteractions(writer) {
                once.println(
                    OBJECT_MAPPER.writeValueAsString(
                        ErrorResponse(
                            errorCode = ErrorCode.API_RATE_LIMIT_EXCEEDED,
                            message = "API rate limit exceeded for write requests"
                        )
                    )
                )
            }
        }
    }
}
