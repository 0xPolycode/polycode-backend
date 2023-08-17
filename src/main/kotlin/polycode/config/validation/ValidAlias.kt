package polycode.config.validation

import javax.validation.Constraint
import javax.validation.Payload
import javax.validation.ReportAsSingleViolation
import javax.validation.constraints.Pattern
import kotlin.reflect.KClass

@Pattern(regexp = "^[A-Za-z0-9_\\-/.]{3,${ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH}}$")
@ReportAsSingleViolation
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [])
annotation class ValidAlias(
    val message: String = "value must be between 3 and 256 characters long and contain only" +
        " letters, digits and characters '-', '_', '.', '/'",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
