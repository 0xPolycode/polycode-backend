package polycode.config.validation

import javax.validation.Constraint
import javax.validation.Payload
import javax.validation.ReportAsSingleViolation
import javax.validation.constraints.Size
import kotlin.reflect.KClass

@Size(max = ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH)
@ReportAsSingleViolation
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.FIELD, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [])
annotation class MaxStringSize(
    val message: String = "size must be between 0 and ${ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH}",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
