package polycode.features.contract.deployment.controller

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import polycode.config.validation.MaxStringSize
import polycode.exception.ResourceNotFoundException
import polycode.features.contract.deployment.model.filters.ContractDecoratorFilters
import polycode.features.contract.deployment.model.json.ArtifactJson
import polycode.features.contract.deployment.model.json.ManifestJson
import polycode.features.contract.deployment.model.response.ArtifactJsonsResponse
import polycode.features.contract.deployment.model.response.ContractDecoratorResponse
import polycode.features.contract.deployment.model.response.ContractDecoratorsResponse
import polycode.features.contract.deployment.model.response.InfoMarkdownsResponse
import polycode.features.contract.deployment.model.response.ManifestJsonsResponse
import polycode.features.contract.deployment.repository.ContractDecoratorRepository
import polycode.features.contract.deployment.repository.ImportedContractDecoratorRepository
import polycode.generated.jooq.id.ProjectId
import polycode.model.filters.parseOrListWithNestedAndLists
import polycode.util.ContractId
import polycode.util.ContractTag
import polycode.util.InterfaceId
import javax.validation.Valid

@Validated
@RestController
class ContractDecoratorController(
    private val contractDecoratorRepository: ContractDecoratorRepository,
    private val importedContractDecoratorRepository: ImportedContractDecoratorRepository
) {

    @GetMapping("/v1/deployable-contracts")
    fun getContractDecorators(
        @Valid @RequestParam("tags", required = false) contractTags: List<@MaxStringSize String>?,
        @Valid @RequestParam("implements", required = false) contractImplements: List<@MaxStringSize String>?,
        @RequestParam("projectId", required = false) projectId: ProjectId?
    ): ResponseEntity<ContractDecoratorsResponse> {
        val filters = ContractDecoratorFilters(
            contractTags = contractTags.parseOrListWithNestedAndLists { ContractTag(it) },
            contractImplements = contractImplements.parseOrListWithNestedAndLists { InterfaceId(it) }
        )
        val contractDecorators = contractDecoratorRepository.getAll(filters) +
            projectId.getIfPresent { importedContractDecoratorRepository.getAll(it, filters) }
        return ResponseEntity.ok(ContractDecoratorsResponse(contractDecorators.map { ContractDecoratorResponse(it) }))
    }

    @GetMapping("/v1/deployable-contracts/manifest.json")
    fun getContractManifestJsonFiles(
        @Valid @RequestParam("tags", required = false) contractTags: List<@MaxStringSize String>?,
        @Valid @RequestParam("implements", required = false) contractImplements: List<@MaxStringSize String>?,
        @RequestParam("projectId", required = false) projectId: ProjectId?
    ): ResponseEntity<ManifestJsonsResponse> {
        val filters = ContractDecoratorFilters(
            contractTags = contractTags.parseOrListWithNestedAndLists { ContractTag(it) },
            contractImplements = contractImplements.parseOrListWithNestedAndLists { InterfaceId(it) }
        )
        val contractManifests = contractDecoratorRepository.getAllManifestJsonFiles(filters) +
            projectId.getIfPresent { importedContractDecoratorRepository.getAllManifestJsonFiles(it, filters) }
        return ResponseEntity.ok(ManifestJsonsResponse(contractManifests))
    }

    @GetMapping("/v1/deployable-contracts/artifact.json")
    fun getContractArtifactJsonFiles(
        @Valid @RequestParam("tags", required = false) contractTags: List<@MaxStringSize String>?,
        @Valid @RequestParam("implements", required = false) contractImplements: List<@MaxStringSize String>?,
        @RequestParam("projectId", required = false) projectId: ProjectId?
    ): ResponseEntity<ArtifactJsonsResponse> {
        val filters = ContractDecoratorFilters(
            contractTags = contractTags.parseOrListWithNestedAndLists { ContractTag(it) },
            contractImplements = contractImplements.parseOrListWithNestedAndLists { InterfaceId(it) }
        )
        val contractArtifacts = contractDecoratorRepository.getAllArtifactJsonFiles(filters) +
            projectId.getIfPresent { importedContractDecoratorRepository.getAllArtifactJsonFiles(it, filters) }
        return ResponseEntity.ok(ArtifactJsonsResponse(contractArtifacts))
    }

    @GetMapping("/v1/deployable-contracts/info.md")
    fun getContractInfoMarkdownFiles(
        @Valid @RequestParam("tags", required = false) contractTags: List<@MaxStringSize String>?,
        @Valid @RequestParam("implements", required = false) contractImplements: List<@MaxStringSize String>?,
        @RequestParam("projectId", required = false) projectId: ProjectId?
    ): ResponseEntity<InfoMarkdownsResponse> {
        val filters = ContractDecoratorFilters(
            contractTags = contractTags.parseOrListWithNestedAndLists { ContractTag(it) },
            contractImplements = contractImplements.parseOrListWithNestedAndLists { InterfaceId(it) }
        )
        val contractInfoMarkdowns = contractDecoratorRepository.getAllInfoMarkdownFiles(filters) +
            projectId.getIfPresent { importedContractDecoratorRepository.getAllInfoMarkdownFiles(it, filters) }
        return ResponseEntity.ok(InfoMarkdownsResponse(contractInfoMarkdowns))
    }

    @GetMapping("/v1/deployable-contracts/{id}")
    fun getContractDecorator(
        @PathVariable("id") id: String,
        @RequestParam("projectId", required = false) projectId: ProjectId?
    ): ResponseEntity<ContractDecoratorResponse> {
        val contractId = ContractId(id)
        val contractDecorator = contractDecoratorRepository.getById(contractId)
            ?: projectId?.let { importedContractDecoratorRepository.getByContractIdAndProjectId(contractId, it) }
            ?: throw ResourceNotFoundException("Contract decorator not found for contract ID: $id")
        return ResponseEntity.ok(ContractDecoratorResponse(contractDecorator))
    }

    @GetMapping("/v1/deployable-contracts/{id}/manifest.json")
    fun getContractManifestJson(
        @PathVariable("id") id: String,
        @RequestParam("projectId", required = false) projectId: ProjectId?
    ): ResponseEntity<ManifestJson> {
        val contractId = ContractId(id)
        val manifestJson = contractDecoratorRepository.getManifestJsonById(contractId)
            ?: projectId
                ?.let { importedContractDecoratorRepository.getManifestJsonByContractIdAndProjectId(contractId, it) }
            ?: throw ResourceNotFoundException("Contract manifest.json not found for contract ID: $id")
        return ResponseEntity.ok(manifestJson)
    }

    @GetMapping("/v1/deployable-contracts/{id}/artifact.json")
    fun getContractArtifactJson(
        @PathVariable("id") id: String,
        @RequestParam("projectId", required = false) projectId: ProjectId?
    ): ResponseEntity<ArtifactJson> {
        val contractId = ContractId(id)
        val artifactJson = contractDecoratorRepository.getArtifactJsonById(contractId)
            ?: projectId
                ?.let { importedContractDecoratorRepository.getArtifactJsonByContractIdAndProjectId(contractId, it) }
            ?: throw ResourceNotFoundException("Contract artifact.json not found for contract ID: $id")
        return ResponseEntity.ok(artifactJson)
    }

    @GetMapping(
        path = ["/v1/deployable-contracts/{id}/info.md"],
        produces = [MediaType.TEXT_MARKDOWN_VALUE]
    )
    fun getContractInfoMarkdown(
        @PathVariable("id") id: String,
        @RequestParam("projectId", required = false) projectId: ProjectId?
    ): ResponseEntity<String> {
        val contractId = ContractId(id)
        val infoMarkdown = contractDecoratorRepository.getInfoMarkdownById(contractId)
            ?: projectId
                ?.let { importedContractDecoratorRepository.getInfoMarkdownByContractIdAndProjectId(contractId, it) }
            ?: throw ResourceNotFoundException("Contract info.md not found for contract ID: $id")
        return ResponseEntity.ok(infoMarkdown)
    }

    private fun <T> ProjectId?.getIfPresent(fn: (ProjectId) -> List<T>): List<T> =
        if (this != null) fn(this) else emptyList()
}
