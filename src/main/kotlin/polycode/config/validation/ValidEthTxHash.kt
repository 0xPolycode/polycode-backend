package polycode.config.validation

import javax.validation.Constraint
import javax.validation.Payload
import javax.validation.ReportAsSingleViolation
import javax.validation.constraints.Pattern
import kotlin.reflect.KClass

@Pattern(regexp = "^(0x)?[A-Fa-f0-9]{1,64}$")
@ReportAsSingleViolation
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [])
annotation class ValidEthTxHash(
    val message: String = "value must be a valid Ethereum transaction hash",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
