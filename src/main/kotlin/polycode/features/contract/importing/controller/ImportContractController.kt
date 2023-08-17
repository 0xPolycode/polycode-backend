package polycode.features.contract.importing.controller

import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import polycode.blockchain.properties.ChainSpec
import polycode.config.binding.annotation.ApiKeyBinding
import polycode.config.interceptors.annotation.ApiReadLimitedMapping
import polycode.config.interceptors.annotation.ApiWriteLimitedMapping
import polycode.config.interceptors.annotation.IdType
import polycode.config.validation.ValidEthAddress
import polycode.features.api.access.model.result.Project
import polycode.features.contract.deployment.model.response.ContractDeploymentRequestResponse
import polycode.features.contract.deployment.service.ContractDeploymentRequestService
import polycode.features.contract.importing.model.params.ImportContractParams
import polycode.features.contract.importing.model.request.ImportContractRequest
import polycode.features.contract.importing.model.response.ImportPreviewResponse
import polycode.features.contract.importing.service.ContractImportService
import polycode.features.contract.interfaces.model.request.ImportedContractInterfacesRequest
import polycode.features.contract.interfaces.model.response.SuggestedContractInterfaceManifestsResponse
import polycode.features.contract.interfaces.service.ContractInterfacesService
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.InterfaceId
import javax.validation.Valid

@Validated
@RestController
class ImportContractController(
    private val contractImportService: ContractImportService,
    private val contractDeploymentRequestService: ContractDeploymentRequestService,
    private val contractInterfacesService: ContractInterfacesService
) {

    @GetMapping("/v1/import-smart-contract/preview/{chainId}/contract/{contractAddress}")
    fun previewSmartContractImport(
        @PathVariable chainId: Long,
        @ValidEthAddress @PathVariable contractAddress: String,
        @RequestParam("customRpcUrl", required = false) customRpcUrl: String?
    ): ResponseEntity<ImportPreviewResponse> {
        val chainSpec = ChainSpec(
            chainId = ChainId(chainId),
            customRpcUrl = customRpcUrl
        )
        val safeContractAddress = ContractAddress(contractAddress)
        val contractDecorator = contractImportService.previewImport(
            contractAddress = safeContractAddress,
            chainSpec = chainSpec
        ).let {
            if (it.implements.isEmpty() && it.id.value.startsWith("imported-")) {
                contractInterfacesService.attachMatchingInterfacesToDecorator(it)
            } else it
        }

        return ResponseEntity.ok(ImportPreviewResponse(contractDecorator))
    }

    @ApiWriteLimitedMapping(IdType.PROJECT_ID, RequestMethod.POST, "/v1/import-smart-contract")
    fun importSmartContract(
        @ApiKeyBinding project: Project,
        @Valid @RequestBody requestBody: ImportContractRequest
    ): ResponseEntity<ContractDeploymentRequestResponse> {
        val params = ImportContractParams(requestBody)
        val importedContractId = contractImportService.importExistingContract(params, project)
            ?: contractImportService.importContract(params, project)
        val importedContract = contractDeploymentRequestService.getContractDeploymentRequest(importedContractId)
        return ResponseEntity.ok(ContractDeploymentRequestResponse(importedContract))
    }

    @ApiReadLimitedMapping(IdType.CONTRACT_DEPLOYMENT_REQUEST_ID, "/v1/import-smart-contract/{id}/suggested-interfaces")
    fun getSuggestedInterfacesForImportedSmartContract(
        @PathVariable("id") id: ContractDeploymentRequestId
    ): ResponseEntity<SuggestedContractInterfaceManifestsResponse> {
        val matchingInterfaces = contractInterfacesService.getSuggestedInterfacesForImportedSmartContract(id)
        return ResponseEntity.ok(SuggestedContractInterfaceManifestsResponse(matchingInterfaces))
    }

    @ApiWriteLimitedMapping(
        idType = IdType.CONTRACT_DEPLOYMENT_REQUEST_ID,
        method = RequestMethod.PATCH,
        path = "/v1/import-smart-contract/{id}/add-interfaces"
    )
    fun addInterfacesToImportedSmartContract(
        @ApiKeyBinding project: Project,
        @PathVariable("id") id: ContractDeploymentRequestId,
        @Valid @RequestBody requestBody: ImportedContractInterfacesRequest
    ): ResponseEntity<ContractDeploymentRequestResponse> {
        contractInterfacesService.addInterfacesToImportedContract(
            importedContractId = id,
            projectId = project.id,
            interfaces = requestBody.interfaces.map { InterfaceId(it) }
        )

        val importedContract = contractDeploymentRequestService.getContractDeploymentRequest(id)

        return ResponseEntity.ok(ContractDeploymentRequestResponse(importedContract))
    }

    @ApiWriteLimitedMapping(
        idType = IdType.CONTRACT_DEPLOYMENT_REQUEST_ID,
        method = RequestMethod.PATCH,
        path = "/v1/import-smart-contract/{id}/remove-interfaces"
    )
    fun removeInterfacesFromImportedSmartContract(
        @ApiKeyBinding project: Project,
        @PathVariable("id") id: ContractDeploymentRequestId,
        @Valid @RequestBody requestBody: ImportedContractInterfacesRequest
    ): ResponseEntity<ContractDeploymentRequestResponse> {
        contractInterfacesService.removeInterfacesFromImportedContract(
            importedContractId = id,
            projectId = project.id,
            interfaces = requestBody.interfaces.map { InterfaceId(it) }
        )

        val importedContract = contractDeploymentRequestService.getContractDeploymentRequest(id)

        return ResponseEntity.ok(ContractDeploymentRequestResponse(importedContract))
    }

    @ApiWriteLimitedMapping(
        idType = IdType.CONTRACT_DEPLOYMENT_REQUEST_ID,
        method = RequestMethod.PATCH,
        path = "/v1/import-smart-contract/{id}/set-interfaces"
    )
    fun setInterfacesForImportedSmartContract(
        @ApiKeyBinding project: Project,
        @PathVariable("id") id: ContractDeploymentRequestId,
        @Valid @RequestBody requestBody: ImportedContractInterfacesRequest
    ): ResponseEntity<ContractDeploymentRequestResponse> {
        contractInterfacesService.setImportedContractInterfaces(
            importedContractId = id,
            projectId = project.id,
            interfaces = requestBody.interfaces.map { InterfaceId(it) }
        )

        val importedContract = contractDeploymentRequestService.getContractDeploymentRequest(id)

        return ResponseEntity.ok(ContractDeploymentRequestResponse(importedContract))
    }
}
