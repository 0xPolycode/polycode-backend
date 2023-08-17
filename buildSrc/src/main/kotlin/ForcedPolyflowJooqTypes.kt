import org.gradle.configurationcache.extensions.capitalized

object ForcedPolyflowJooqTypes {

    data class JooqType(
        val userType: String,
        val includeExpression: String,
        val includeTypes: String,
        val converter: String
    ) {
        constructor(userType: String, includeExpression: String, includeTypes: String) : this(
            userType, includeExpression, includeTypes, userType + "Converter"
        )
    }

    private val domainIdTypes = listOf(
        "POLYFLOW_USER_ID"
    )

    val types = listOf(
        JooqType(
            userType = "polycode.util.UtcDateTime",
            includeExpression = ".*",
            includeTypes = "TIMESTAMPTZ"
        )
    ) + domainIdTypes.map {
        val typeName = it.split("_").joinToString("") { it.toLowerCase().capitalized() }

        JooqType(
            userType = "polyflow.generated.jooq.id.$typeName",
            converter = "polyflow.generated.jooq.converters.${typeName}Converter",
            includeExpression = ".*",
            includeTypes = it
        )
    }
}
