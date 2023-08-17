package polycode.config.authentication

import mu.KLogging
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UsernameNotFoundException
import polycode.exception.JwtTokenException
import java.security.interfaces.RSAPublicKey

class JwtAuthenticationProvider(private val publicKey: RSAPublicKey) : AuthenticationProvider {

    companion object : KLogging()

    override fun authenticate(authentication: Authentication): Authentication {
        val token = authentication.credentials

        if (token is String) {
            try {
                return JwtTokenUtils.decodeToken(token, publicKey)
            } catch (e: JwtTokenException) {
                logger.info(e) { "Invalid JWT" }
                SecurityContextHolder.clearContext()
                throw e
            }
        }

        throw UsernameNotFoundException("Authentication is missing JWT token.")
    }

    override fun supports(authentication: Class<*>): Boolean = authentication == JwtAuthToken::class.java
}
