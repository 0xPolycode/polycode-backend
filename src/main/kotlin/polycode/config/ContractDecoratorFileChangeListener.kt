package polycode.config

import com.fasterxml.jackson.databind.DatabindException
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import org.springframework.boot.devtools.filewatch.ChangedFiles
import org.springframework.boot.devtools.filewatch.FileChangeListener
import polycode.exception.ContractDecoratorException
import polycode.exception.ContractInterfaceNotFoundException
import polycode.features.contract.deployment.model.json.ArtifactJson
import polycode.features.contract.deployment.model.json.InterfaceManifestJson
import polycode.features.contract.deployment.model.json.ManifestJson
import polycode.features.contract.deployment.model.result.ContractDecorator
import polycode.features.contract.deployment.repository.ContractDecoratorRepository
import polycode.features.contract.deployment.repository.ContractMetadataRepository
import polycode.features.contract.interfaces.repository.ContractInterfacesRepository
import polycode.generated.jooq.id.ContractMetadataId
import polycode.model.result.ContractMetadata
import polycode.service.UuidProvider
import polycode.util.Constants
import polycode.util.ContractId
import polycode.util.InterfaceId
import java.io.File
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.relativeTo
import kotlin.reflect.KClass

@Suppress("TooManyFunctions")
class ContractDecoratorFileChangeListener(
    private val uuidProvider: UuidProvider,
    private val contractDecoratorRepository: ContractDecoratorRepository,
    private val contractInterfacesRepository: ContractInterfacesRepository,
    private val contractMetadataRepository: ContractMetadataRepository,
    private val objectMapper: ObjectMapper,
    private val contractsDir: Path,
    private val interfacesDir: Path?,
    private val ignoredDirs: List<String>
) : FileChangeListener {

    companion object : KLogging()

    init {
        interfacesDir?.let { processNestedInterfaces(it, it) }

        contractsDir.listDirectoryEntries()
            .filter { it.filterDirs() }
            .forEach { set ->
                logger.info { "Processing contract decorators in ${set.name}..." }
                set.listDirectoryEntries()
                    .filter { entry -> entry.filterDirs() }
                    .forEach { dir -> processNestedDecorators(dir, emptyList(), set.name) }
            }
    }

    @Suppress("MagicNumber")
    override fun onChange(changeSet: Set<ChangedFiles>) {
        val (decoratorChanges, interfaceChanges) = changeSet.partition { it.sourceDirectory.toPath() == contractsDir }

        onContractDecoratorChange(decoratorChanges)
        interfacesDir?.let { onContractInterfaceChange(it, interfaceChanges) }
    }

    private fun onContractDecoratorChange(changeSet: List<ChangedFiles>) {
        logger.info { "Detected contract decorator changes: $changeSet" }

        val changedDirs = changeSet.flatMap {
            it.files.mapNotNull { file -> file.relativeName.split('/').dropLast(1) }
        }.distinct()

        logger.info { "Detected contract directory changes: $changedDirs" }

        changedDirs
            .filter { it.isNotEmpty() }
            .forEach {
                val setName = it.first()
                val parts = it.drop(1).dropLast(1)
                val decorator = it.last()
                val contractDecoratorDir = contractsDir.resolve(setName)
                    .resolve(parts.joinToString("/"))
                    .resolve(decorator)

                processContractDecorator(contractDecoratorDir, parts, setName)
            }
    }

    private fun onContractInterfaceChange(interfacesRootDir: Path, changeSet: List<ChangedFiles>) {
        logger.info { "Detected contract interface changes: $changeSet" }

        changeSet.flatMap { it.files }
            .map {
                val changedPath = it.file.toPath()

                if (changedPath.name.endsWith("info.md")) {
                    changedPath.parent.resolve(changedPath.name.removeSuffix("info.md") + "manifest.json")
                } else {
                    changedPath
                }
            }
            .forEach { processContractInterface(interfacesRootDir, it) }
    }

    private fun Path.filterManifestFiles(): Boolean = this.isRegularFile() && this.name.endsWith("manifest.json")

    private fun Path.filterDirs(): Boolean = this.isDirectory() && !ignoredDirs.contains(this.name)

    private fun processNestedInterfaces(interfacesRootDir: Path, dir: Path) {
        dir.listDirectoryEntries()
            .forEach {
                if (it.filterManifestFiles()) {
                    processContractInterface(interfacesRootDir, it)
                } else if (it.filterDirs()) {
                    processNestedInterfaces(interfacesRootDir, it)
                }
            }
    }

    private fun processContractInterface(interfacesRootDir: Path, manifest: Path) {
        val relativePath = manifest.relativeTo(interfacesRootDir)
        val id = InterfaceId(relativePath.toString().removeSuffix("manifest.json").removeSuffix(".").removeSuffix("/"))
        logger.info { "Processing contract interface $id..." }

        val infoMd = manifest.parent.resolve(manifest.name.removeSuffix("manifest.json") + "info.md").toFile()
        val infoMarkdown = infoMd.takeIf { it.isFile }?.readText() ?: ""
        val manifestJson = objectMapper.tryParse(id.value, "interface", manifest.toFile(), InterfaceManifestJson::class)

        if (manifestJson != null) {
            contractInterfacesRepository.store(id, manifestJson)
            contractInterfacesRepository.store(id, infoMarkdown)
        } else {
            contractInterfacesRepository.delete(id)
        }
    }

    private fun processNestedDecorators(dir: Path, parts: List<String>, setName: String) {
        if (dir.resolve("artifact.json").isRegularFile() || dir.resolve("manifest.json").isRegularFile()) {
            processContractDecorator(dir, parts, setName)
        } else if (dir.filterDirs()) {
            dir.listDirectoryEntries().forEach { processNestedDecorators(it, parts + dir.name, setName) }
        }
    }

    private fun processContractDecorator(contractDecoratorDir: Path, parts: List<String>, setName: String) {
        val nestedParts = parts.joinToString("/")
        val id = ContractId("$setName/$nestedParts/${contractDecoratorDir.name}".replace("//", "/"))
        logger.info { "Processing contract decorator $id..." }

        val artifact = contractDecoratorDir.resolve("artifact.json").toFile()
        val manifest = contractDecoratorDir.resolve("manifest.json").toFile()
        val infoMd = contractDecoratorDir.resolve("info.md").toFile()
        val artifactJson = objectMapper.tryParse(id.value, "decorator", artifact, ArtifactJson::class)
        val manifestJson = objectMapper.tryParse(id.value, "decorator", manifest, ManifestJson::class)
        val infoMarkdown = infoMd.takeIf { it.isFile }?.readText() ?: ""

        if (artifactJson != null && manifestJson != null) {
            try {
                val decorator = ContractDecorator(
                    id = id,
                    artifact = artifactJson,
                    manifest = manifestJson,
                    imported = false,
                    interfacesProvider = interfacesDir?.let { contractInterfacesRepository::getById }
                )

                contractDecoratorRepository.store(decorator)
                contractDecoratorRepository.store(decorator.id, manifestJson)
                contractDecoratorRepository.store(decorator.id, artifactJson)
                contractDecoratorRepository.store(decorator.id, infoMarkdown)
                contractMetadataRepository.createOrUpdate(
                    ContractMetadata(
                        id = uuidProvider.getUuid(ContractMetadataId),
                        name = decorator.name,
                        description = decorator.description,
                        contractId = decorator.id,
                        contractTags = decorator.tags,
                        contractImplements = decorator.implements,
                        projectId = Constants.NIL_PROJECT_ID
                    )
                )
            } catch (e: ContractDecoratorException) {
                logger.warn(e) { "${e.message} for contract decorator: $id, skipping..." }
                contractDecoratorRepository.delete(id)
            } catch (e: ContractInterfaceNotFoundException) {
                logger.warn(e) { "${e.message} for contract decorator: $id, skipping..." }
                contractDecoratorRepository.delete(id)
            }
        } else {
            contractDecoratorRepository.delete(id)
        }
    }

    private fun <T : Any> ObjectMapper.tryParse(id: String, type: String, file: File, valueType: KClass<T>): T? =
        if (file.isFile) {
            try {
                readValue(file, valueType.java)
            } catch (e: DatabindException) {
                logger.warn(e) { "Unable to parse ${file.name} for contract $type: $id, skipping..." }
                null
            }
        } else {
            logger.warn { "${file.name} is missing for contract $type: $id, skipping..." }
            null
        }
}
