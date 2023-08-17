package polycode.config.authentication

import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import polycode.util.UtcDateTime

class JwtAuthToken(
    val token: String,
    val id: String?, // TODO allow UserId | WalletAddress
    val email: String?,
    val validUntil: UtcDateTime
) : Authentication {

    companion object {
        private const val serialVersionUID: Long = -6797137239150015604L
    }

    override fun getName(): String = id.toString()

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> = mutableListOf()

    override fun getCredentials(): String = token

    override fun getDetails(): Any? = null

    override fun getPrincipal(): String? = id

    override fun isAuthenticated(): Boolean = id != null

    override fun setAuthenticated(isAuthenticated: Boolean) {
        // not needed
    }
}
