package polycode.features.contract.deployment.model.json

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonNaming
import polycode.util.InterfaceId

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class ManifestJson(
    val name: String?,
    val description: String?,
    @JsonDeserialize(`as` = LinkedHashSet::class)
    val tags: Set<String>,
    @JsonDeserialize(`as` = LinkedHashSet::class)
    val implements: Set<String>,
    val eventDecorators: List<EventDecorator>,
    val constructorDecorators: List<ConstructorDecorator>,
    val functionDecorators: List<FunctionDecorator>
) {
    companion object {
        val EMPTY = ManifestJson("", "", emptySet(), emptySet(), emptyList(), emptyList(), emptyList())
    }
}

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class InterfaceManifestJson(
    val name: String?,
    val description: String?,
    @JsonDeserialize(`as` = LinkedHashSet::class)
    val tags: Set<String>,
    val eventDecorators: List<EventDecorator>,
    val functionDecorators: List<FunctionDecorator>
)

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class InterfaceManifestJsonWithId(
    val id: InterfaceId,
    val name: String?,
    val description: String?,
    @JsonDeserialize(`as` = LinkedHashSet::class)
    val tags: Set<String>,
    val matchingEventDecorators: List<EventDecorator>,
    val matchingFunctionDecorators: List<FunctionDecorator>
)

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class TypeDecorator(
    val name: String,
    val description: String,
    val recommendedTypes: List<String>,
    val parameters: List<TypeDecorator>?,
    val hints: List<Any>?
)

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class EventTypeDecorator(
    val name: String,
    val description: String,
    val indexed: Boolean,
    val recommendedTypes: List<String>,
    val parameters: List<TypeDecorator>?,
    val hints: List<Any>?
)

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class ReturnTypeDecorator(
    val name: String,
    val description: String,
    val solidityType: String,
    val recommendedTypes: List<String>,
    val parameters: List<ReturnTypeDecorator>?,
    val hints: List<Any>?
)

interface OverridableDecorator {
    val signature: String
}

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class EventDecorator(
    override val signature: String,
    val name: String,
    val description: String,
    val parameterDecorators: List<EventTypeDecorator>
) : OverridableDecorator

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class ConstructorDecorator(
    val signature: String,
    val description: String,
    val parameterDecorators: List<TypeDecorator>
)

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class FunctionDecorator(
    override val signature: String,
    val name: String,
    val description: String,
    val parameterDecorators: List<TypeDecorator>,
    val returnDecorators: List<ReturnTypeDecorator>,
    val emittableEvents: List<String>,
    val readOnly: Boolean
) : OverridableDecorator
