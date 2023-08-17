package polycode.config.validation

import javax.validation.Constraint
import javax.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [MaxJsonNodeCharsValidator::class])
annotation class MaxJsonNodeChars(
    val maxChars: Int = ValidationConstants.REQUEST_BODY_MAX_JSON_CHARS,
    val message: String = "value must be a valid JSON of at most {maxChars} characters",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
