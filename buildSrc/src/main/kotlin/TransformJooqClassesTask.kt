import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.configurationcache.extensions.capitalized
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

abstract class TransformJooqClassesTask : DefaultTask() {

    companion object {
        private data class RecordClassInfo(val name: String, val properties: Map<String, RecordProperty>)

        private data class RecordProperty(val name: String, val type: String, val index: Int, val nonNull: Boolean) {
            val actualType = if (nonNull) type else "${type}?"
        }

        private data class TableClassInfo(val className: String, val valueName: String)

        private data class DomainTypeInfo(val name: String, val typeDefinition: String, val converterDefinition: String)

        private const val PLACEHOLDER = "<< PLACEHOLDER >>"
        private val ALL_PLACEHOLDERS_REGEX = "($PLACEHOLDER\n)+".toRegex()
        private val RECORD_PROPERTY_REGEX = "[ ]+var[^\n]+\n[ ]+set[^\n]+\n([ ]+@NotNull\n)?[ ]+get[^\n]+\n".toRegex()
        private val RECORD_PROPERTY_VALUES_REGEX =
            "var ([^:]+): ([^\n]+)\n[ ]+set\\(value\\): Unit = set\\((\\d+), value\\)".toRegex()
        private val RECORD_CONSTRUCTOR_REGEX = "constructor\\([^)]+\\)".toRegex()
        private val TABLE_COLUMN_REGEX =
            "val ([^:]+): TableField<([^,]+), (Array<[^>]+>[^>]+|[^>]+)>( = createField[^\n]+)".toRegex()
        private val UPPERCASE_TO_CAMEL_CASE_REGEX = "_([a-z])".toRegex()
        private val COMPANION_OBJECT_REGEX = "companion object \\{\n\n[ ]+val ([^:]+):[^\n]+\n[ ]+}".toRegex()
        private val DOMAIN_NAME_AND_TYPE_REGEX = "val ([A-Z_0-9]+): Domain<([A-Za-z0-9]+)>.+".toRegex()
    }

    @get:Input
    abstract val jooqClassesPath: Property<String>

    @get:Input
    abstract val packageName: Property<String>

    @TaskAction
    fun transformJooqClasses() {
        val rootPath = Paths.get(jooqClassesPath.get())

        val recordInfos = transformRecordClasses(rootPath)
        val tableInfos = transformTableClasses(rootPath, recordInfos)

        renameTableReferences(rootPath, tableInfos)

        generateConverter(rootPath)
        generateDomainValueClasses(rootPath)
    }

    private fun transformRecordClasses(rootPath: Path): Map<String, RecordClassInfo> =
        (rootPath.resolve("tables/records")
            .toFile().listFiles() ?: throw RuntimeException("no files found in path: $rootPath"))
            .filter { it.isFile }
            .map { transformRecordClass(it.toPath()) }
            .associateBy { it.name }

    private fun transformRecordClass(path: Path): RecordClassInfo {
        val recordSource = Files.readString(path)

        val properties = RECORD_PROPERTY_REGEX.findAll(recordSource).map {
            val (_, propertyName, propertyType, index) = RECORD_PROPERTY_VALUES_REGEX.find(it.value)!!.groupValues
            val nonNull = it.value.contains("@NotNull")
            RecordProperty(propertyName, propertyType.replace("?", ""), index.toInt(), nonNull)
        }

        val propertiesSource = properties.joinToString("\n") {
            """|    var ${it.name}: ${it.actualType}
               |        private set(value): Unit = set(${it.index}, value)
               |        get(): ${it.actualType} = get(${it.index}) as ${it.actualType}
               |""".trimMargin()
        }
        val constructorProperties = properties.joinToString(prefix = "constructor(", separator = ", ", postfix = ")") {
            "${it.name}: ${it.actualType}"
        }

        val modifiedSource = recordSource
            .replace(RECORD_PROPERTY_REGEX, PLACEHOLDER)
            .replace(ALL_PLACEHOLDERS_REGEX, PLACEHOLDER)
            .replace(PLACEHOLDER, propertiesSource)
            .replace(RECORD_CONSTRUCTOR_REGEX, constructorProperties)

        Files.writeString(path, modifiedSource)

        return RecordClassInfo(path.fileName.toString().removeSuffix(".kt"), properties.associateBy { it.name })
    }

    private fun transformTableClasses(rootPath: Path, recordInfos: Map<String, RecordClassInfo>): List<TableClassInfo> =
        (rootPath.resolve("tables")
            .toFile().listFiles() ?: throw RuntimeException("no files found in path: $rootPath"))
            .filter { it.isFile }
            .map { transformTableClass(it.toPath(), recordInfos) }

    private fun transformTableClass(path: Path, recordInfos: Map<String, RecordClassInfo>): TableClassInfo {
        val recordSource = Files.readString(path)
        val tableClassName = path.fileName.toString().removeSuffix(".kt")
        var tableValueName = ""

        val modifiedSource = recordSource
            .replace(TABLE_COLUMN_REGEX) {
                val (_, uppercaseName, recordName, _, rest) = it.groupValues
                val record = recordInfos[recordName]!!
                val camelCaseName = UPPERCASE_TO_CAMEL_CASE_REGEX
                    .replace(uppercaseName.toLowerCase()) { it.groupValues[1].toUpperCase() }
                    .removeSuffix("_")
                val field = record.properties[camelCaseName]!!

                "val $uppercaseName: TableField<$recordName, ${field.actualType}>$rest"
            }
            .replace(COMPANION_OBJECT_REGEX) {
                val (_, valueName) = it.groupValues
                tableValueName = valueName
                "companion object : $tableClassName()"
            }

        Files.writeString(path, modifiedSource)

        return TableClassInfo(tableClassName, tableValueName)
    }

    private fun renameTableReferences(rootPath: Path, tableInfos: List<TableClassInfo>) {
        val replacements = tableInfos.map { Pair("${it.className}.${it.valueName}", it.className) }
        val recordFiles = rootPath.resolve("tables/records").toFile().listFiles()!!.asSequence()
        val tableFiles = rootPath.resolve("tables").toFile().listFiles()!!.asSequence()
        val baseFiles = rootPath.toFile().listFiles()!!.asSequence()

        sequenceOf(recordFiles, tableFiles, baseFiles)
            .flatMap { it }.filter { it.isFile }
            .map { it.toPath() }
            .forEach { path ->
                val source = Files.readString(path)
                val modifiedSource = replacements.fold(source) { acc, pair -> acc.replace(pair.first, pair.second) }
                Files.writeString(path, modifiedSource)
            }
    }

    private fun generateConverter(rootPath: Path) {
        val code =
            """|package ${packageName.get()}.converters
               |
               |import org.jooq.Converter
               |
               |class KotlinConverter<T : Any, U : Any>(
               |    val from: Class<T>,
               |    val to: Class<U>,
               |    val fromMapping: (T) -> U?,
               |    val toMapping: (U) -> T?
               |) : Converter<T, U> {
               |    override fun from(value: T?): U? = value?.let { fromMapping(it) }
               |    override fun to(value: U?): T? = value?.let { toMapping(it) }
               |    override fun fromType(): Class<T> = from
               |    override fun toType(): Class<U> = to
               |
               |    companion object {
               |        private const val serialVersionUID: Long = 7668375669351902507L
               |    }
               |}
               |
               |inline fun <reified T : Any, reified U : Any> converter(
               |    noinline fromMapping: (T) -> U?,
               |    noinline toMapping: (U) -> T?
               |): Converter<T, U> = KotlinConverter(
               |    from = T::class.java,
               |    to = U::class.java,
               |    fromMapping = fromMapping,
               |    toMapping = toMapping
               |)
               |""".trimMargin()

        Files.createDirectories(rootPath.resolve("converters"))
        Files.writeString(rootPath.resolve("converters/KotlinConverter.kt"), code, StandardOpenOption.CREATE)
    }

    private fun generateDomainValueClasses(rootPath: Path) {
        val domainsFile = rootPath.resolve("domains/Domains.kt")
        val domainsSource = Files.readAllLines(domainsFile)

        val domains = domainsSource.filter { it.startsWith("val ") && it.contains("Domain<") }
            .map {
                val (_, domainName, domainType) = DOMAIN_NAME_AND_TYPE_REGEX.find(it)!!.groupValues
                val camelCaseDomainName = domainName.split("_").joinToString("") { it.toLowerCase().capitalized() }

                val typeDefinition =
                    """|@JvmInline
                       |value class $camelCaseDomainName(override val value: $domainType) : DatabaseId {
                       |    companion object : DatabaseIdWrapper<$camelCaseDomainName> {
                       |        override fun wrap(uuid: UUID) = $camelCaseDomainName(uuid)
                       |    }
                       |}
                       |""".trimMargin()
                val converterDefinition = "fun ${camelCaseDomainName}Converter() = converter(" +
                    "{ it: $domainType -> $camelCaseDomainName(it) }, { it.value })"

                DomainTypeInfo(
                    name = camelCaseDomainName,
                    typeDefinition = typeDefinition,
                    converterDefinition = converterDefinition
                )
            }

        val idTypesString =
            """|package ${packageName.get()}.id
               |
               |import java.util.UUID
               |
               |interface DatabaseId {
               |    val value: UUID
               |}
               |
               |interface DatabaseIdWrapper<T> {
               |    fun wrap(uuid: UUID): T
               |}
               |
               |${domains.joinToString("\n") { it.typeDefinition }}
               |""".trimMargin()

        Files.createDirectories(rootPath.resolve("id"))
        Files.writeString(rootPath.resolve("id/GeneratedIds.kt"), idTypesString, StandardOpenOption.CREATE)

        val idTypesSerializers = domains.joinToString("\n") {
            "        addSerializer(${it.name}::class.java, GeneratedIdSerializer)\n" +
                "        addDeserializer(${it.name}::class.java, GeneratedIdDeserializer(${it.name}))"
        }
        val jacksonModuleString =
            """|package ${packageName.get()}.id
               |
               |import com.fasterxml.jackson.core.JsonGenerator
               |import com.fasterxml.jackson.core.JsonParser
               |import com.fasterxml.jackson.databind.DeserializationContext
               |import com.fasterxml.jackson.databind.JsonDeserializer
               |import com.fasterxml.jackson.databind.JsonSerializer
               |import com.fasterxml.jackson.databind.SerializerProvider
               |import com.fasterxml.jackson.databind.deser.std.UUIDDeserializer
               |import com.fasterxml.jackson.databind.module.SimpleModule
               |
               |private object GeneratedIdSerializer : JsonSerializer<DatabaseId>() {
               |    override fun serialize(value: DatabaseId, gen: JsonGenerator, serializers: SerializerProvider) {
               |        gen.writeString(value.value.toString())
               |    }
               |}
               |
               |private val uuidDeserializer = UUIDDeserializer()
               |
               |private class GeneratedIdDeserializer<T : DatabaseId>(
               |    private val wrapper: DatabaseIdWrapper<T>
               |) : JsonDeserializer<T>() {
               |    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): T =
               |        wrapper.wrap(uuidDeserializer.deserialize(p, ctxt))
               |}
               |
               |object GeneratedIdsJacksonModule : SimpleModule() {
               |    private const val serialVersionUID: Long = -7167909219104494624L
               |
               |    init {
               |$idTypesSerializers
               |    }
               |}
               |
               |""".trimMargin()


        Files.writeString(
            rootPath.resolve("id/GeneratedIdsJacksonModule.kt"),
            jacksonModuleString,
            StandardOpenOption.CREATE
        )

        val convertersString =
            """|package ${packageName.get()}.converters
               |
               |import java.util.UUID
               |import ${packageName.get()}.id.*
               |
               |${domains.joinToString("\n") { it.converterDefinition }}
               |""".trimMargin()

        Files.writeString(
            rootPath.resolve("converters/GeneratedConverters.kt"),
            convertersString,
            StandardOpenOption.CREATE
        )
    }
}
