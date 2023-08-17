package polycode

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.PropertyNamingStrategies.NamingBase
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.victools.jsonschema.generator.CustomPropertyDefinition
import com.github.victools.jsonschema.generator.FieldScope
import com.github.victools.jsonschema.generator.Option
import com.github.victools.jsonschema.generator.OptionPreset
import com.github.victools.jsonschema.generator.SchemaGenerator
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder
import com.github.victools.jsonschema.generator.SchemaVersion
import polycode.config.JsonConfig
import polycode.util.annotation.SchemaAnyOf
import polycode.util.annotation.SchemaIgnore
import polycode.util.annotation.SchemaName
import polycode.util.annotation.SchemaNotNull
import java.lang.reflect.Type
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

object JsonSchemaDocumentation {

    private val OBJECT_MAPPER = JsonConfig().objectMapper()
    private val BIG_INTEGER_TYPE_NODE = OBJECT_MAPPER.readTree(
        "{\"type\":\"string\",\"pattern\":\"^[+-]?[0-9]+$\"}"
    ) as ObjectNode

    private val generator = SchemaGeneratorConfigBuilder(
        OBJECT_MAPPER,
        SchemaVersion.DRAFT_2020_12,
        OptionPreset.PLAIN_JSON
    ).apply {
        forFields().apply {
            withCustomDefinitionProvider { field, _ ->
                if (field.declaredType.isInstanceOf(BigInteger::class.java)) {
                    CustomPropertyDefinition(BIG_INTEGER_TYPE_NODE)
                } else {
                    null
                }
            }
            withCustomDefinitionProvider { field, ctx ->
                if (parseClass(field.type.typeName)?.getAnnotation(SchemaAnyOf::class.java) != null) {
                    val definition = ctx.createDefinition(field.type)
                    val anyOfList = objectMapper.createArrayNode().apply {
                        definition["properties"]?.elements()?.forEach { this.add(it) }
                    }

                    CustomPropertyDefinition(
                        objectMapper.createObjectNode().apply {
                            set<JsonNode>("anyOf", anyOfList)
                        }
                    )
                } else {
                    null
                }
            }
            withPropertyNameOverrideResolver { field ->
                val nameOverride = field.getAnnotation(SchemaName::class.java)?.name
                val namingStrategy = field.getNamingStrategy()
                nameOverride ?: namingStrategy.translate(field.name)
            }
            withNullableCheck { field ->
                field.getAnnotation(SchemaNotNull::class.java)?.let { false } ?: field.isNullable()
            }
            withIgnoreCheck { field ->
                field.getAnnotation(SchemaIgnore::class.java) != null
            }
        }
        with(
            Option.ADDITIONAL_FIXED_TYPES,
            Option.EXTRA_OPEN_API_FORMAT_VALUES
        )
    }.let { SchemaGenerator(it.build()) }

    private fun FieldScope.getNamingStrategy(): NamingBase =
        Class.forName(declaringType.typeName).getAnnotation(JsonNaming::class.java)
            ?.value?.constructors?.toList()?.getOrNull(0)?.call() as? NamingBase
            ?: PropertyNamingStrategies.SnakeCaseStrategy()

    private fun FieldScope.isNullable() = Class.forName(this.declaringType.typeName)
        .kotlin.members.find { it.name == this.declaredName }?.returnType?.isMarkedNullable ?: false

    private fun parseClass(typeName: String): Class<*>? = try {
        Class.forName(typeName)
    } catch (_: ClassNotFoundException) {
        null
    }

    private fun flattenSchemaDef(defs: ObjectNode, defName: String) {
        val anyOf = defs[defName]?.get("properties")?.get("types")?.get("anyOf")?.deepCopy<JsonNode>()

        anyOf?.let {
            defs.set<JsonNode>(
                defName,
                OBJECT_MAPPER.createObjectNode().apply {
                    set<JsonNode>("anyOf", anyOf)
                }
            )
        }
    }

    fun createSchema(type: Type) {
        Files.createDirectories(Paths.get("build/generated-snippets"))

        val prettySchema = generator.generateSchema(type).apply {
            (this["\$defs"] as? ObjectNode)?.let {
                flattenSchemaDef(it, "FunctionArgumentTypes")
                flattenSchemaDef(it, "ReturnValueTypes")
            }
        }.toPrettyString()

        Files.writeString(
            Paths.get("build/generated-snippets/${type.typeName}.adoc"),
            "[%collapsible]\n" +
                "====\n" +
                "[source,options=\"nowrap\"]\n" +
                "----\n" +
                "${prettySchema}\n" +
                "----\n" +
                "====\n",
            StandardOpenOption.CREATE
        )
    }
}
