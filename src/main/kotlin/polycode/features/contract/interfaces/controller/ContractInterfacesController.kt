package polycode.features.contract.interfaces.controller

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import polycode.config.validation.MaxStringSize
import polycode.exception.ResourceNotFoundException
import polycode.features.contract.deployment.model.response.InfoMarkdownsResponse
import polycode.features.contract.interfaces.model.filters.ContractInterfaceFilters
import polycode.features.contract.interfaces.model.response.ContractInterfaceManifestResponse
import polycode.features.contract.interfaces.model.response.ContractInterfaceManifestsResponse
import polycode.features.contract.interfaces.repository.ContractInterfacesRepository
import polycode.model.filters.parseOrListWithNestedAndLists
import polycode.util.ContractTag
import polycode.util.InterfaceId
import javax.validation.Valid

@Validated
@RestController
class ContractInterfacesController(
    private val contractInterfacesRepository: ContractInterfacesRepository
) {

    @GetMapping("/v1/contract-interfaces")
    fun getContractInterfaces(
        @Valid @RequestParam("tags", required = false) contractTags: List<@MaxStringSize String>?
    ): ResponseEntity<ContractInterfaceManifestsResponse> {
        val filters = ContractInterfaceFilters(
            interfaceTags = contractTags.parseOrListWithNestedAndLists { ContractTag(it) }
        )
        val contractInterfaces = contractInterfacesRepository.getAll(filters)
        return ResponseEntity.ok(
            ContractInterfaceManifestsResponse(
                contractInterfaces.map { ContractInterfaceManifestResponse(it) }
            )
        )
    }

    @GetMapping("/v1/contract-interfaces/info.md")
    fun getContractInterfaceInfoMarkdownFiles(
        @Valid @RequestParam("tags", required = false) contractTags: List<@MaxStringSize String>?
    ): ResponseEntity<InfoMarkdownsResponse> {
        val filters = ContractInterfaceFilters(
            interfaceTags = contractTags.parseOrListWithNestedAndLists { ContractTag(it) }
        )
        val interfaceInfoMarkdowns = contractInterfacesRepository.getAllInfoMarkdownFiles(filters)
        return ResponseEntity.ok(InfoMarkdownsResponse(interfaceInfoMarkdowns))
    }

    @GetMapping("/v1/contract-interfaces/{id}")
    fun getContractInterface(
        @PathVariable("id") id: String
    ): ResponseEntity<ContractInterfaceManifestResponse> {
        val interfaceId = InterfaceId(id)
        val contractInterface = contractInterfacesRepository.getById(interfaceId)
            ?: throw ResourceNotFoundException("Contract interface not found for interface ID: $id")
        return ResponseEntity.ok(ContractInterfaceManifestResponse(interfaceId, contractInterface))
    }

    @GetMapping(
        path = ["/v1/contract-interfaces/{id}/info.md"],
        produces = [MediaType.TEXT_MARKDOWN_VALUE]
    )
    fun getContractInterfaceInfoMarkdown(
        @PathVariable("id") id: String
    ): ResponseEntity<String> {
        val interfaceId = InterfaceId(id)
        val infoMarkdown = contractInterfacesRepository.getInfoMarkdownById(interfaceId)
            ?: throw ResourceNotFoundException("Contract interface info.md not found for interface ID: $id")
        return ResponseEntity.ok(infoMarkdown)
    }
}
