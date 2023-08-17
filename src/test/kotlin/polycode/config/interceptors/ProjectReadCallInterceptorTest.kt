package polycode.config.interceptors

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerMapping
import polycode.TestBase
import polycode.TestData
import polycode.config.JsonConfig
import polycode.config.interceptors.annotation.ApiReadLimitedMapping
import polycode.config.interceptors.annotation.IdType
import polycode.exception.ErrorCode
import polycode.exception.ErrorResponse
import polycode.features.api.usage.repository.ApiRateLimitRepository
import polycode.features.api.usage.repository.UserIdResolverRepository
import polycode.generated.jooq.id.UserId
import polycode.service.UtcDateTimeProvider
import java.io.PrintWriter
import java.util.UUID
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class ProjectReadCallInterceptorTest : TestBase() {

    companion object {
        // @formatter:off
        @Suppress("unused")
        fun nonAnnotated() {}
        @ApiReadLimitedMapping(IdType.PROJECT_ID, "/test-path/{projectId}")
        @Suppress("unused", "UNUSED_PARAMETER")
        fun annotated(@PathVariable projectId: String) {}
        // @formatter:on

        private val OBJECT_MAPPER = JsonConfig().objectMapper()
        private val PATH_VARIABLES = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE
    }

    @Test
    fun mustNotHandleNonAnnotatedMethod() {
        val apiRateLimitRepository = mock<ApiRateLimitRepository>()
        val userIdResolverRepository = mock<UserIdResolverRepository>()
        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        val handler = HandlerMethod(Companion, Companion::class.java.methods.find { it.name == "nonAnnotated" }!!)
        val interceptor = ProjectReadCallInterceptor(
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
                apiRateLimitRepository,
                userIdResolverRepository,
                utcDateTimeProvider,
                request,
                response
            )
        }
    }

    @Test
    fun mustCorrectlyHandleAnnotatedMethodWhenThereIsSomeRemainingReadLimitAndReturnStatusIsSuccess() {
        val projectId = UUID.randomUUID()
        val idType = IdType.PROJECT_ID
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
            call(apiRateLimitRepository.remainingReadLimit(userId, TestData.TIMESTAMP))
                .willReturn(1)
        }

        val response = mock<HttpServletResponse>()

        suppose("response will have successful status") {
            call(response.status)
                .willReturn(HttpStatus.OK.value())
        }

        val handler = HandlerMethod(Companion, Companion::class.java.methods.find { it.name == "annotated" }!!)
        val interceptor = ProjectReadCallInterceptor(
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

            expectInteractions(apiRateLimitRepository) {
                once.remainingReadLimit(userId, TestData.TIMESTAMP)
                once.addReadCall(userId, TestData.TIMESTAMP, "/test-path/{projectId}")
            }

            expectInteractions(userIdResolverRepository) {
                twice.getUserId(idType, projectId)
            }
        }
    }

    @Test
    fun mustCorrectlyHandleAnnotatedMethodWhenThereIsNoRemainingReadLimitAndReturnStatusIsNonSuccess() {
        val projectId = UUID.randomUUID()
        val idType = IdType.PROJECT_ID
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
            call(apiRateLimitRepository.remainingReadLimit(userId, TestData.TIMESTAMP))
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

        val handler = HandlerMethod(Companion, Companion::class.java.methods.find { it.name == "annotated" }!!)
        val interceptor = ProjectReadCallInterceptor(
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

            expectInteractions(apiRateLimitRepository) {
                once.remainingReadLimit(userId, TestData.TIMESTAMP)
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
                            message = "API rate limit exceeded for read requests"
                        )
                    )
                )
            }
        }
    }
}
