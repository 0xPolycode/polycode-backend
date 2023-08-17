package polycode.config.authentication

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.web.AuthenticationEntryPoint
import polycode.exception.ErrorCode
import polycode.exception.ErrorResponse
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class AuthenticationEntryPointExceptionHandler(private val objectMapper: ObjectMapper) : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest?,
        response: HttpServletResponse?,
        authException: AuthenticationException?
    ) {
        val errorMessage = when (authException) {
            is BadCredentialsException -> "Bad credentials"
            is UsernameNotFoundException -> "User not found"
            else -> "Authentication failed"
        }

        val errorResponse = ErrorResponse(
            errorCode = ErrorCode.BAD_AUTHENTICATION,
            message = errorMessage
        )

        response?.status = HttpStatus.UNAUTHORIZED.value()
        response?.contentType = MediaType.APPLICATION_JSON_VALUE
        response?.outputStream?.print(objectMapper.writeValueAsString(errorResponse))
    }
}
