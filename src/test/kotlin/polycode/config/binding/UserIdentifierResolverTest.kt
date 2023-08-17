package polycode.config.binding

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.core.MethodParameter
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import polycode.TestBase
import polycode.config.binding.annotation.UserIdentifierBinding
import polycode.exception.BadAuthenticationException
import polycode.features.api.access.model.result.UserIdentifier
import polycode.features.api.access.model.result.UserWalletAddressIdentifier
import polycode.features.api.access.repository.UserIdentifierRepository
import polycode.generated.jooq.id.UserId
import polycode.service.UuidProvider
import polycode.util.WalletAddress
import java.util.UUID

class UserIdentifierResolverTest : TestBase() { // TODO more tests

    companion object {
        // @formatter:off
        @Suppress("unused", "UNUSED_PARAMETER")
        fun supportedMethod(@UserIdentifierBinding param: UserIdentifier) {}
        @Suppress("unused", "UNUSED_PARAMETER")
        fun unsupportedMethod1(param: UserIdentifier) {}
        @Suppress("unused", "UNUSED_PARAMETER")
        fun unsupportedMethod2(@UserIdentifierBinding param: String) {}
        // @formatter:on
    }

    @Test
    fun mustSupportAnnotatedUserIdentifierParameter() {
        val resolver = UserIdentifierResolver(mock(), mock(), mock(), mock(), mock())

        verify("annotated UserIdentifier parameter is supported") {
            val method = Companion::class.java.methods.find { it.name == "supportedMethod" }!!
            val parameter = MethodParameter(method, 0)

            expectThat(resolver.supportsParameter(parameter))
                .isTrue()
        }
    }

    @Test
    fun mustNotSupportUnannotatedUserIdentifierParameter() {
        val resolver = UserIdentifierResolver(mock(), mock(), mock(), mock(), mock())

        verify("annotated UserIdentifier parameter is supported") {
            val method = Companion::class.java.methods.find { it.name == "unsupportedMethod1" }!!
            val parameter = MethodParameter(method, 0)

            expectThat(resolver.supportsParameter(parameter))
                .isFalse()
        }
    }

    @Test
    fun mustNotSupportAnnotatedNonUserIdentifierParameter() {
        val resolver = UserIdentifierResolver(mock(), mock(), mock(), mock(), mock())

        verify("annotated UserIdentifier parameter is supported") {
            val method = Companion::class.java.methods.find { it.name == "unsupportedMethod2" }!!
            val parameter = MethodParameter(method, 0)

            expectThat(resolver.supportsParameter(parameter))
                .isFalse()
        }
    }

    @Test
    fun mustCorrectlyFetchExistingUserIdentifierForWalletAddress() {
        val walletAddress = WalletAddress("abc123")
        val authentication = mock<Authentication>()

        suppose("authentication principal will some wallet address") {
            call(authentication.principal)
                .willReturn(walletAddress.rawValue)
        }

        val securityContext = mock<SecurityContext>()

        suppose("security context will return some authentication object") {
            call(securityContext.authentication)
                .willReturn(authentication)
            SecurityContextHolder.setContext(securityContext)
        }

        val repository = mock<UserIdentifierRepository>()
        val identifier = UserWalletAddressIdentifier(
            id = UserId(UUID.randomUUID()),
            walletAddress = walletAddress
        )

        suppose("user wallet address is fetched from database") {
            call(repository.getByWalletAddress(walletAddress))
                .willReturn(identifier)
        }

        val resolver = UserIdentifierResolver(mock(), mock(), repository, mock(), mock())

        verify("user identifier is correctly returned") {
            expectThat(resolver.resolveArgument(mock(), mock(), mock(), mock()))
                .isEqualTo(identifier)
        }
    }

    @Test
    fun mustCorrectlyCreateNewUserIdentifierForWalletAddress() {
        val walletAddress = WalletAddress("abc123")
        val authentication = mock<Authentication>()

        suppose("authentication principal will some wallet address") {
            call(authentication.principal)
                .willReturn(walletAddress.rawValue)
        }

        val securityContext = mock<SecurityContext>()

        suppose("security context will return some authentication object") {
            call(securityContext.authentication)
                .willReturn(authentication)
            SecurityContextHolder.setContext(securityContext)
        }

        val repository = mock<UserIdentifierRepository>()

        suppose("user wallet address is not in database") {
            call(repository.getByWalletAddress(walletAddress))
                .willReturn(null)
        }

        val uuidProvider = mock<UuidProvider>()
        val uuid = UserId(UUID.randomUUID())

        suppose("some UUID will be returned") {
            call(uuidProvider.getUuid(UserId))
                .willReturn(uuid)
        }

        val identifier = UserWalletAddressIdentifier(
            id = uuid,
            walletAddress = walletAddress
        )

        suppose("user identifier will be stored in database") {
            call(repository.store(identifier))
                .willReturn(identifier)
        }

        val resolver = UserIdentifierResolver(uuidProvider, mock(), repository, mock(), mock())

        verify("user identifier is correctly returned") {
            expectThat(resolver.resolveArgument(mock(), mock(), mock(), mock()))
                .isEqualTo(identifier)
        }
    }

    @Test
    fun mustThrowBadAuthenticationExceptionWhenAuthenticationPrincipalIsNotAString() {
        val authentication = mock<Authentication>()

        suppose("authentication principal will return some non-string object") {
            call(authentication.principal)
                .willReturn(emptyList<Nothing>())
        }

        val securityContext = mock<SecurityContext>()

        suppose("security context will return some authentication object") {
            call(securityContext.authentication)
                .willReturn(authentication)
            SecurityContextHolder.setContext(securityContext)
        }

        val resolver = UserIdentifierResolver(mock(), mock(), mock(), mock(), mock())

        verify("BadAuthenticationException is thrown") {
            expectThrows<BadAuthenticationException> {
                resolver.resolveArgument(mock(), mock(), mock(), mock())
            }
        }
    }

    @Test
    fun mustThrowBadAuthenticationExceptionWhenAuthenticationPrincipalIsNull() {
        val authentication = mock<Authentication>()

        suppose("authentication principal is null") {
            call(authentication.principal)
                .willReturn(null)
        }

        val securityContext = mock<SecurityContext>()

        suppose("security context will return some authentication object") {
            call(securityContext.authentication)
                .willReturn(authentication)
            SecurityContextHolder.setContext(securityContext)
        }

        val resolver = UserIdentifierResolver(mock(), mock(), mock(), mock(), mock())

        verify("BadAuthenticationException is thrown") {
            expectThrows<BadAuthenticationException> {
                resolver.resolveArgument(mock(), mock(), mock(), mock())
            }
        }
    }

    @Test
    fun mustThrowBadAuthenticationExceptionWhenAuthenticationIsNull() {
        val securityContext = mock<SecurityContext>()

        suppose("security context authentication is null") {
            call(securityContext.authentication)
                .willReturn(null)
            SecurityContextHolder.setContext(securityContext)
        }

        val resolver = UserIdentifierResolver(mock(), mock(), mock(), mock(), mock())

        verify("BadAuthenticationException is thrown") {
            expectThrows<BadAuthenticationException> {
                resolver.resolveArgument(mock(), mock(), mock(), mock())
            }
        }
    }
}
