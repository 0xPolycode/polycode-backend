package polycode.features.contract.deployment.repository

import mu.KLogging
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import polycode.features.contract.deployment.model.filters.ContractDecoratorFilters
import polycode.features.contract.deployment.model.json.ArtifactJson
import polycode.features.contract.deployment.model.json.ManifestJson
import polycode.features.contract.deployment.model.result.ContractDecorator
import polycode.features.contract.interfaces.repository.ContractInterfacesRepository
import polycode.generated.jooq.id.ImportedContractDecoratorId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.tables.ImportedContractDecoratorTable
import polycode.generated.jooq.tables.records.ImportedContractDecoratorRecord
import polycode.model.filters.AndList
import polycode.model.filters.OrList
import polycode.util.ContractId
import polycode.util.ContractTag
import polycode.util.InterfaceId
import polycode.util.UtcDateTime

@Repository
@Suppress("TooManyFunctions")
class JooqImportedContractDecoratorRepository(
    private val dslContext: DSLContext,
    private val contractInterfacesRepository: ContractInterfacesRepository
) : ImportedContractDecoratorRepository {

    companion object : KLogging()

    override fun store(
        id: ImportedContractDecoratorId,
        projectId: ProjectId,
        contractId: ContractId,
        manifestJson: ManifestJson,
        artifactJson: ArtifactJson,
        infoMarkdown: String,
        importedAt: UtcDateTime,
        previewOnly: Boolean
    ): ContractDecorator {
        logger.info {
            "Store imported contract decorator, id: $id, projectId: $projectId, contractId: $contractId," +
                " previewOnly: $previewOnly"
        }

        if (!previewOnly) {
            val record = ImportedContractDecoratorRecord(
                id = id,
                projectId = projectId,
                contractId = contractId,
                manifestJson = manifestJson,
                artifactJson = artifactJson,
                infoMarkdown = infoMarkdown,
                contractTags = manifestJson.tags.toTypedArray(),
                contractImplements = manifestJson.implements.toTypedArray(),
                importedAt = importedAt
            )

            dslContext.executeInsert(record)
        }

        return ContractDecorator(
            id = contractId,
            artifact = artifactJson,
            manifest = manifestJson,
            imported = true,
            interfacesProvider = contractInterfacesRepository::getById
        )
    }

    override fun updateInterfaces(
        contractId: ContractId,
        projectId: ProjectId,
        interfaces: List<InterfaceId>,
        manifest: ManifestJson
    ): Boolean {
        logger.info {
            "Update imported contract decorator interfaces, contractId: $contractId, projectId: $projectId," +
                " interfaces: $interfaces"
        }
        return dslContext.update(ImportedContractDecoratorTable)
            .set(ImportedContractDecoratorTable.CONTRACT_IMPLEMENTS, interfaces.map { it.value }.toTypedArray())
            .set(
                ImportedContractDecoratorTable.MANIFEST_JSON,
                manifest.copy(implements = interfaces.map { it.value }.toSet())
            )
            .where(
                DSL.and(
                    ImportedContractDecoratorTable.CONTRACT_ID.eq(contractId),
                    ImportedContractDecoratorTable.PROJECT_ID.eq(projectId)
                )
            )
            .execute() > 0
    }

    override fun getByContractIdAndProjectId(contractId: ContractId, projectId: ProjectId): ContractDecorator? {
        logger.debug { "Get imported contract decorator by contract id: $contractId" }
        return dslContext.selectFrom(ImportedContractDecoratorTable)
            .where(
                DSL.and(
                    ImportedContractDecoratorTable.CONTRACT_ID.eq(contractId),
                    ImportedContractDecoratorTable.PROJECT_ID.eq(projectId)
                )
            )
            .fetchOne()
            ?.let {
                ContractDecorator(
                    id = it.contractId,
                    artifact = it.artifactJson,
                    manifest = it.manifestJson,
                    imported = true,
                    interfacesProvider = contractInterfacesRepository::getById
                )
            }
    }

    override fun getManifestJsonByContractIdAndProjectId(contractId: ContractId, projectId: ProjectId): ManifestJson? {
        logger.debug { "Get imported manifest.json by contract id: $contractId, project id: $projectId" }
        return dslContext.select(ImportedContractDecoratorTable.MANIFEST_JSON)
            .from(ImportedContractDecoratorTable)
            .where(
                DSL.and(
                    ImportedContractDecoratorTable.CONTRACT_ID.eq(contractId),
                    ImportedContractDecoratorTable.PROJECT_ID.eq(projectId)
                )
            )
            .fetchOne()
            ?.value1()
    }

    override fun getArtifactJsonByContractIdAndProjectId(contractId: ContractId, projectId: ProjectId): ArtifactJson? {
        logger.debug { "Get imported artifact.json by contract id: $contractId, project id: $projectId" }
        return dslContext.select(ImportedContractDecoratorTable.ARTIFACT_JSON)
            .from(ImportedContractDecoratorTable)
            .where(
                DSL.and(
                    ImportedContractDecoratorTable.CONTRACT_ID.eq(contractId),
                    ImportedContractDecoratorTable.PROJECT_ID.eq(projectId)
                )
            )
            .fetchOne()
            ?.value1()
    }

    override fun getInfoMarkdownByContractIdAndProjectId(contractId: ContractId, projectId: ProjectId): String? {
        logger.debug { "Get imported info.md by contract id: $contractId, project id: $projectId" }
        return dslContext.select(ImportedContractDecoratorTable.INFO_MARKDOWN)
            .from(ImportedContractDecoratorTable)
            .where(
                DSL.and(
                    ImportedContractDecoratorTable.CONTRACT_ID.eq(contractId),
                    ImportedContractDecoratorTable.PROJECT_ID.eq(projectId)
                )
            )
            .fetchOne()
            ?.value1()
    }

    override fun getAll(projectId: ProjectId, filters: ContractDecoratorFilters): List<ContractDecorator> {
        logger.debug { "Get imported contract decorators by projectId: $projectId, filters: $filters" }
        return dslContext.selectFrom(ImportedContractDecoratorTable)
            .where(createConditions(projectId, filters))
            .orderBy(ImportedContractDecoratorTable.IMPORTED_AT.asc())
            .fetch {
                ContractDecorator(
                    id = it.contractId,
                    artifact = it.artifactJson,
                    manifest = it.manifestJson,
                    imported = true,
                    interfacesProvider = contractInterfacesRepository::getById
                )
            }
    }

    override fun getAllManifestJsonFiles(projectId: ProjectId, filters: ContractDecoratorFilters): List<ManifestJson> {
        logger.debug { "Get imported manifest.json files by projectId: $projectId, filters: $filters" }
        return dslContext.select(ImportedContractDecoratorTable.MANIFEST_JSON)
            .from(ImportedContractDecoratorTable)
            .where(createConditions(projectId, filters))
            .orderBy(ImportedContractDecoratorTable.IMPORTED_AT.asc())
            .fetch { it.value1() }
    }

    override fun getAllArtifactJsonFiles(projectId: ProjectId, filters: ContractDecoratorFilters): List<ArtifactJson> {
        logger.debug { "Get imported artifact.json files by projectId: $projectId, filters: $filters" }
        return dslContext.select(ImportedContractDecoratorTable.ARTIFACT_JSON)
            .from(ImportedContractDecoratorTable)
            .where(createConditions(projectId, filters))
            .orderBy(ImportedContractDecoratorTable.IMPORTED_AT.asc())
            .fetch { it.value1() }
    }

    override fun getAllInfoMarkdownFiles(projectId: ProjectId, filters: ContractDecoratorFilters): List<String> {
        logger.debug { "Get imported info.md files by projectId: $projectId, filters: $filters" }
        return dslContext.select(ImportedContractDecoratorTable.INFO_MARKDOWN)
            .from(ImportedContractDecoratorTable)
            .where(createConditions(projectId, filters))
            .orderBy(ImportedContractDecoratorTable.IMPORTED_AT.asc())
            .fetch { it.value1() }
    }

    private fun createConditions(projectId: ProjectId, filters: ContractDecoratorFilters) =
        listOfNotNull(
            ImportedContractDecoratorTable.PROJECT_ID.eq(projectId),
            filters.contractTags.orAndCondition { it.contractTagsAndCondition() },
            filters.contractImplements.orAndCondition { it.contractTraitsAndCondition() }
        )

    private fun AndList<ContractTag>.contractTagsAndCondition(): Condition? =
        takeIf { list.isNotEmpty() }?.let {
            ImportedContractDecoratorTable.CONTRACT_TAGS.contains(
                it.list.map { v -> v.value }.toTypedArray()
            )
        }

    private fun AndList<InterfaceId>.contractTraitsAndCondition(): Condition? =
        takeIf { list.isNotEmpty() }?.let {
            ImportedContractDecoratorTable.CONTRACT_IMPLEMENTS.contains(
                it.list.map { v -> v.value }.toTypedArray()
            )
        }

    private fun <T> OrList<AndList<T>>.orAndCondition(innerConditionMapping: (AndList<T>) -> Condition?): Condition? =
        list.mapNotNull(innerConditionMapping).takeIf { it.isNotEmpty() }?.let { DSL.or(it) }
}
