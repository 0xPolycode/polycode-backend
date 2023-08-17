package polycode.config.validation

import org.hibernate.validator.constraints.CompositionType
import org.hibernate.validator.constraints.ConstraintComposition
import javax.validation.Constraint
import javax.validation.Payload
import javax.validation.ReportAsSingleViolation
import javax.validation.constraints.DecimalMax
import javax.validation.constraints.PositiveOrZero
import kotlin.reflect.KClass

@PositiveOrZero
@DecimalMax(ValidationConstants.UINT_256_MAX)
@ReportAsSingleViolation
@ConstraintComposition(CompositionType.AND)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [])
annotation class ValidUint256(
    val message: String = "value must be within range [0, 2^256]",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
