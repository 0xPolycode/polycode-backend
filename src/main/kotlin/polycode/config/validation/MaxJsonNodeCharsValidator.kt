package polycode.config.validation

import com.fasterxml.jackson.databind.JsonNode
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorContext

class MaxJsonNodeCharsValidator : ConstraintValidator<MaxJsonNodeChars, JsonNode> {

    private lateinit var parameters: MaxJsonNodeChars

    override fun initialize(parameters: MaxJsonNodeChars) {
        this.parameters = parameters
    }

    override fun isValid(value: JsonNode?, context: ConstraintValidatorContext): Boolean {
        val numChars = value?.toString()?.length ?: 0
        return numChars <= parameters.maxChars
    }
}
