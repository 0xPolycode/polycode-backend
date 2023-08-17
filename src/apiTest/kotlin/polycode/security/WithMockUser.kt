package polycode.security

import org.springframework.security.test.context.support.WithSecurityContext
import polycode.testcontainers.HardhatTestContainer

@Retention(value = AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
@WithSecurityContext(factory = WithMockUserSecurityFactory::class)
annotation class WithMockUser(
    val address: String = HardhatTestContainer.ACCOUNT_ADDRESS_1
)
