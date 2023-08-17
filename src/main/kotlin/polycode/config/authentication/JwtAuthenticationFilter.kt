package polycode.config.authentication

import org.springframework.http.HttpHeaders
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import polycode.util.UtcDateTime
import java.time.OffsetDateTime
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JwtAuthenticationFilter : OncePerRequestFilter() {

    private val prefix = "Bearer "

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val header = request.getHeader(HttpHeaders.AUTHORIZATION)

        if (header != null && header.startsWith(prefix)) {
            val authToken = header.removePrefix(prefix)
            SecurityContextHolder.getContext().authentication = JwtAuthToken(
                token = authToken,
                id = null,
                email = null,
                validUntil = UtcDateTime(OffsetDateTime.now())
            )
        }

        filterChain.doFilter(request, response)
    }
}
