package polycode.features.contract.deployment.controller

import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import polycode.config.binding.annotation.ApiKeyBinding
import polycode.config.interceptors.annotation.ApiReadLimitedMapping
import polycode.config.interceptors.annotation.ApiWriteLimitedMapping
import polycode.config.interceptors.annotation.IdType
import polycode.config.validation.MaxStringSize
import polycode.features.api.access.model.result.Project
import polycode.features.contract.deployment.model.filters.ContractDeploymentRequestFilters
import polycode.features.contract.deployment.model.params.CreateContractDeploymentRequestParams
import polycode.features.contract.deployment.model.request.CreateContractDeploymentRequest
import polycode.features.contract.deployment.model.response.ContractDeploymentRequestResponse
import polycode.features.contract.deployment.model.response.ContractDeploymentRequestsResponse
import polycode.features.contract.deployment.service.ContractDeploymentRequestService
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.model.filters.OrList
import polycode.model.filters.parseOrListWithNestedAndLists
import polycode.model.request.AttachTransactionInfoRequest
import polycode.util.ContractId
import polycode.util.ContractTag
import polycode.util.InterfaceId
import polycode.util.TransactionHash
import polycode.util.WalletAddress
import javax.validation.Valid

@Validated
@RestController
class ContractDeploymentRequestController(
    private val contractDeploymentRequestService: ContractDeploymentRequestService
) {

    @ApiWriteLimitedMapping(IdType.PROJECT_ID, RequestMethod.POST, "/v1/deploy")
    fun createContractDeploymentRequest(
        @ApiKeyBinding project: Project,
        @Valid @RequestBody requestBody: CreateContractDeploymentRequest
    ): ResponseEntity<ContractDeploymentRequestResponse> {
        val params = CreateContractDeploymentRequestParams(requestBody)
        val createdRequest = contractDeploymentRequestService.createContractDeploymentRequest(params, project)
        return ResponseEntity.ok(ContractDeploymentRequestResponse(createdRequest))
    }

    @DeleteMapping("/v1/deploy/{id}")
    fun markContractDeploymentRequestAsDeleted(
        @ApiKeyBinding project: Project,
        @PathVariable("id") id: ContractDeploymentRequestId
    ) {
        contractDeploymentRequestService.markContractDeploymentRequestAsDeleted(id, project.id)
    }

    @ApiReadLimitedMapping(IdType.CONTRACT_DEPLOYMENT_REQUEST_ID, "/v1/deploy/{id}")
    fun getContractDeploymentRequest(
        @PathVariable("id") id: ContractDeploymentRequestId
    ): ResponseEntity<ContractDeploymentRequestResponse> {
        val contractDeploymentRequest = contractDeploymentRequestService.getContractDeploymentRequest(id)
        return ResponseEntity.ok(ContractDeploymentRequestResponse(contractDeploymentRequest))
    }

    @ApiReadLimitedMapping(IdType.PROJECT_ID, "/v1/deploy/by-project/{projectId}/by-alias/{alias}")
    fun getContractDeploymentRequestByProjectIdAndAlias(
        @PathVariable("projectId") projectId: ProjectId,
        @PathVariable("alias") alias: String
    ): ResponseEntity<ContractDeploymentRequestResponse> {
        val contractDeploymentRequest = contractDeploymentRequestService
            .getContractDeploymentRequestByProjectIdAndAlias(
                projectId = projectId,
                alias = alias
            )
        return ResponseEntity.ok(ContractDeploymentRequestResponse(contractDeploymentRequest))
    }

    @ApiReadLimitedMapping(IdType.PROJECT_ID, "/v1/deploy/by-project/{projectId}")
    fun getContractDeploymentRequestsByProjectIdAndFilters(
        @PathVariable("projectId") projectId: ProjectId,
        @Valid @RequestParam("contractIds", required = false) contractIds: List<@MaxStringSize String>?,
        @Valid @RequestParam("contractTags", required = false) contractTags: List<@MaxStringSize String>?,
        @Valid @RequestParam("contractImplements", required = false) contractImplements: List<@MaxStringSize String>?,
        @RequestParam("deployedOnly", required = false, defaultValue = "false") deployedOnly: Boolean,
    ): ResponseEntity<ContractDeploymentRequestsResponse> {
        val contractDeploymentRequests = contractDeploymentRequestService
            .getContractDeploymentRequestsByProjectIdAndFilters(
                projectId = projectId,
                filters = ContractDeploymentRequestFilters(
                    contractIds = OrList(contractIds.orEmpty().map { ContractId(it) }),
                    contractTags = contractTags.parseOrListWithNestedAndLists { ContractTag(it) },
                    contractImplements = contractImplements.parseOrListWithNestedAndLists { InterfaceId(it) },
                    deployedOnly = deployedOnly
                )
            )
        return ResponseEntity.ok(
            ContractDeploymentRequestsResponse(contractDeploymentRequests.map { ContractDeploymentRequestResponse(it) })
        )
    }

    @ApiWriteLimitedMapping(IdType.CONTRACT_DEPLOYMENT_REQUEST_ID, RequestMethod.PUT, "/v1/deploy/{id}")
    fun attachTransactionInfo(
        @PathVariable("id") id: ContractDeploymentRequestId,
        @Valid @RequestBody requestBody: AttachTransactionInfoRequest
    ) {
        contractDeploymentRequestService.attachTxInfo(
            id = id,
            txHash = TransactionHash(requestBody.txHash),
            deployer = WalletAddress(requestBody.callerAddress)
        )
    }
}
