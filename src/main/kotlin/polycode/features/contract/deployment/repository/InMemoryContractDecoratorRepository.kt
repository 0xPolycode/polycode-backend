package polycode.features.contract.deployment.repository

import mu.KLogging
import org.springframework.stereotype.Repository
import polycode.features.contract.deployment.model.filters.ContractDecoratorFilters
import polycode.features.contract.deployment.model.json.ArtifactJson
import polycode.features.contract.deployment.model.json.ManifestJson
import polycode.features.contract.deployment.model.result.ContractDecorator
import polycode.model.filters.AndList
import polycode.model.filters.OrList
import polycode.util.ContractId
import java.util.concurrent.ConcurrentHashMap

@Repository
@Suppress("TooManyFunctions")
class InMemoryContractDecoratorRepository : ContractDecoratorRepository {

    companion object : KLogging()

    private val storage = ConcurrentHashMap<ContractId, ContractDecorator>()
    private val manifestJsonStorage = ConcurrentHashMap<ContractId, ManifestJson>()
    private val artifactJsonStorage = ConcurrentHashMap<ContractId, ArtifactJson>()
    private val infoMarkdownStorage = ConcurrentHashMap<ContractId, String>()

    override fun store(contractDecorator: ContractDecorator): ContractDecorator {
        logger.info { "Storing contract decorator with ID: ${contractDecorator.id}" }
        storage[contractDecorator.id] = contractDecorator
        return contractDecorator
    }

    override fun store(id: ContractId, manifestJson: ManifestJson): ManifestJson {
        logger.info { "Storing contract manifest.json with ID: $id" }
        manifestJsonStorage[id] = manifestJson
        return manifestJson
    }

    override fun store(id: ContractId, artifactJson: ArtifactJson): ArtifactJson {
        logger.info { "Storing contract artifact.json with ID: $id" }
        artifactJsonStorage[id] = artifactJson
        return artifactJson
    }

    override fun store(id: ContractId, infoMd: String): String {
        logger.info { "Storing contract info.md with ID: $id" }
        infoMarkdownStorage[id] = infoMd
        return infoMd
    }

    override fun delete(id: ContractId): Boolean {
        logger.info { "Deleting contract decorator with ID: $id" }
        manifestJsonStorage.remove(id)
        artifactJsonStorage.remove(id)
        infoMarkdownStorage.remove(id)
        return storage.remove(id) != null
    }

    override fun getById(id: ContractId): ContractDecorator? {
        logger.debug { "Get contract decorator by ID: $id" }
        return storage[id]
    }

    override fun getManifestJsonById(id: ContractId): ManifestJson? {
        logger.debug { "Get contract manifest.json by ID: $id" }
        return manifestJsonStorage[id]
    }

    override fun getArtifactJsonById(id: ContractId): ArtifactJson? {
        logger.debug { "Get contract artifact.json by ID: $id" }
        return artifactJsonStorage[id]
    }

    override fun getInfoMarkdownById(id: ContractId): String? {
        logger.debug { "Get contract info.md by ID: $id" }
        return infoMarkdownStorage[id]
    }

    override fun getAll(filters: ContractDecoratorFilters): List<ContractDecorator> {
        logger.debug { "Get all contract decorators, filters: $filters" }
        return storage.values
            .filterBy(filters.contractTags) { it.tags }
            .filterBy(filters.contractImplements) { it.implements }
            .toList()
    }

    override fun getAllManifestJsonFiles(filters: ContractDecoratorFilters): List<ManifestJson> {
        logger.debug { "Get all contract manifest.json files, filters: $filters" }
        return getAll(filters).mapNotNull { manifestJsonStorage[it.id] }
    }

    override fun getAllArtifactJsonFiles(filters: ContractDecoratorFilters): List<ArtifactJson> {
        logger.debug { "Get all contract artifact.json files, filters: $filters" }
        return getAll(filters).mapNotNull { artifactJsonStorage[it.id] }
    }

    override fun getAllInfoMarkdownFiles(filters: ContractDecoratorFilters): List<String> {
        logger.debug { "Get all contract info.md files, filters: $filters" }
        return getAll(filters).mapNotNull { infoMarkdownStorage[it.id] }
    }

    private fun <T> Collection<ContractDecorator>.filterBy(
        orList: OrList<AndList<T>>,
        values: (ContractDecorator) -> List<T>
    ): Collection<ContractDecorator> {
        val conditions = orList.list.map { it.list }

        return if (conditions.isEmpty()) {
            this
        } else {
            filter { decorator ->
                val decoratorValues = values(decorator)
                conditions.map { condition -> decoratorValues.containsAll(condition) }.contains(true)
            }
        }
    }
}
