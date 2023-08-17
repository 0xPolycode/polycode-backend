package polycode.config.interceptors

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.web.servlet.HandlerMapping
import polycode.TestBase
import polycode.config.interceptors.annotation.IdType
import polycode.features.api.usage.repository.UserIdResolverRepository
import polycode.generated.jooq.id.UserId
import java.util.UUID
import javax.servlet.http.HttpServletRequest

class UserIdResolverTest : TestBase() {

    companion object {
        private val PATH_VARIABLES = HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE
    }

    @Test
    fun mustCorrectlyResolveSomeIdToUserId() {
        val id = UUID.randomUUID()
        val userId = UserId(UUID.randomUUID())
        val idType = IdType.ASSET_SEND_REQUEST_ID
        val userIdResolverRepository = mock<UserIdResolverRepository>()

        suppose("userId will be resolved in the repository") {
            call(userIdResolverRepository.getUserId(idType, id))
                .willReturn(userId)
        }

        val request = mock<HttpServletRequest>()

        suppose("request will contain id") {
            call(request.getAttribute(PATH_VARIABLES))
                .willReturn(mapOf(idType.idVariableName to id.toString()))
        }

        verify("userId is correctly resolved") {
            val resolvedUserId = UserIdResolver.resolve(
                userIdResolverRepository = userIdResolverRepository,
                interceptorName = "test",
                request = request,
                idType = idType,
                path = "/test-path/{${idType.idVariableName}}/rest"
            )

            expectThat(resolvedUserId)
                .isEqualTo(userId)
        }
    }

    @Test
    fun mustThrowIllegalStateExceptionWhenIdIsNotPresentInTheRequest() {
        val id = UUID.randomUUID()
        val userId = UserId(UUID.randomUUID())
        val idType = IdType.ASSET_SEND_REQUEST_ID
        val userIdResolverRepository = mock<UserIdResolverRepository>()

        suppose("userId will be resolved in the repository") {
            call(userIdResolverRepository.getUserId(idType, id))
                .willReturn(userId)
        }

        val request = mock<HttpServletRequest>()

        suppose("request will not contain id") {
            call(request.getAttribute(PATH_VARIABLES))
                .willReturn(emptyMap<String, String>())
        }

        verify("IllegalStateException is thrown") {
            expectThrows<IllegalStateException> {
                UserIdResolver.resolve(
                    userIdResolverRepository = userIdResolverRepository,
                    interceptorName = "test",
                    request = request,
                    idType = idType,
                    path = "/test-path/{${idType.idVariableName}}/rest"
                )
            }
        }
    }

    @Test
    fun mustReturnNullWhenRequestIdIsNotParsable() {
        val idType = IdType.ASSET_SEND_REQUEST_ID
        val request = mock<HttpServletRequest>()

        suppose("request will contain id") {
            call(request.getAttribute(PATH_VARIABLES))
                .willReturn(mapOf(idType.idVariableName to "invalid-id"))
        }

        verify("userId is correctly resolved") {
            val resolvedUserId = UserIdResolver.resolve(
                userIdResolverRepository = mock(),
                interceptorName = "test",
                request = request,
                idType = idType,
                path = "/test-path/{${idType.idVariableName}}/rest"
            )

            expectThat(resolvedUserId)
                .isNull()
        }
    }
}
