package polycode.features.contract.deployment.repository

import polycode.features.contract.deployment.model.filters.ContractDecoratorFilters
import polycode.features.contract.deployment.model.json.ArtifactJson
import polycode.features.contract.deployment.model.json.ManifestJson
import polycode.features.contract.deployment.model.result.ContractDecorator
import polycode.generated.jooq.id.ImportedContractDecoratorId
import polycode.generated.jooq.id.ProjectId
import polycode.util.ContractId
import polycode.util.InterfaceId
import polycode.util.UtcDateTime

interface ImportedContractDecoratorRepository {
    @Suppress("LongParameterList")
    fun store(
        id: ImportedContractDecoratorId,
        projectId: ProjectId,
        contractId: ContractId,
        manifestJson: ManifestJson,
        artifactJson: ArtifactJson,
        infoMarkdown: String,
        importedAt: UtcDateTime,
        previewOnly: Boolean
    ): ContractDecorator

    fun updateInterfaces(
        contractId: ContractId,
        projectId: ProjectId,
        interfaces: List<InterfaceId>,
        manifest: ManifestJson
    ): Boolean

    fun getByContractIdAndProjectId(contractId: ContractId, projectId: ProjectId): ContractDecorator?
    fun getManifestJsonByContractIdAndProjectId(contractId: ContractId, projectId: ProjectId): ManifestJson?
    fun getArtifactJsonByContractIdAndProjectId(contractId: ContractId, projectId: ProjectId): ArtifactJson?
    fun getInfoMarkdownByContractIdAndProjectId(contractId: ContractId, projectId: ProjectId): String?
    fun getAll(projectId: ProjectId, filters: ContractDecoratorFilters): List<ContractDecorator>
    fun getAllManifestJsonFiles(projectId: ProjectId, filters: ContractDecoratorFilters): List<ManifestJson>
    fun getAllArtifactJsonFiles(projectId: ProjectId, filters: ContractDecoratorFilters): List<ArtifactJson>
    fun getAllInfoMarkdownFiles(projectId: ProjectId, filters: ContractDecoratorFilters): List<String>
}
