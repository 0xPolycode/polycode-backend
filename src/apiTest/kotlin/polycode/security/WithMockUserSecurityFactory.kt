package polycode.security

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.test.context.support.WithSecurityContextFactory
import polycode.util.Right
import polycode.util.WalletAddress

class WithMockUserSecurityFactory : WithSecurityContextFactory<WithMockUser> {

    override fun createSecurityContext(annotation: WithMockUser): SecurityContext {
        val token = UsernamePasswordAuthenticationToken(Right(WalletAddress(annotation.address)), "password", null)
        val context = SecurityContextHolder.createEmptyContext()
        context.authentication = token
        return context
    }
}
