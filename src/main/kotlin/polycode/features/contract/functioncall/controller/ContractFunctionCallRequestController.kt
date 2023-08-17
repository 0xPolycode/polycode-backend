package polycode.features.contract.functioncall.controller

import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import polycode.config.binding.annotation.ApiKeyBinding
import polycode.config.interceptors.annotation.ApiReadLimitedMapping
import polycode.config.interceptors.annotation.ApiWriteLimitedMapping
import polycode.config.interceptors.annotation.IdType
import polycode.config.validation.ValidEthAddress
import polycode.features.api.access.model.result.Project
import polycode.features.contract.functioncall.model.filters.ContractFunctionCallRequestFilters
import polycode.features.contract.functioncall.model.params.CreateContractFunctionCallRequestParams
import polycode.features.contract.functioncall.model.request.CreateContractFunctionCallRequest
import polycode.features.contract.functioncall.model.response.ContractFunctionCallRequestResponse
import polycode.features.contract.functioncall.model.response.ContractFunctionCallRequestsResponse
import polycode.features.contract.functioncall.service.ContractFunctionCallRequestService
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.generated.jooq.id.ContractFunctionCallRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.model.request.AttachTransactionInfoRequest
import polycode.util.ContractAddress
import polycode.util.TransactionHash
import polycode.util.WalletAddress
import javax.validation.Valid

@Validated
@RestController
class ContractFunctionCallRequestController(
    private val contractFunctionCallRequestService: ContractFunctionCallRequestService
) {

    @ApiWriteLimitedMapping(IdType.PROJECT_ID, RequestMethod.POST, "/v1/function-call")
    fun createContractFunctionCallRequest(
        @ApiKeyBinding project: Project,
        @Valid @RequestBody requestBody: CreateContractFunctionCallRequest
    ): ResponseEntity<ContractFunctionCallRequestResponse> {
        val params = CreateContractFunctionCallRequestParams(requestBody)
        val createdRequest = contractFunctionCallRequestService.createContractFunctionCallRequest(params, project)
        return ResponseEntity.ok(ContractFunctionCallRequestResponse(createdRequest))
    }

    @ApiReadLimitedMapping(IdType.FUNCTION_CALL_REQUEST_ID, "/v1/function-call/{id}")
    fun getContractFunctionCallRequest(
        @PathVariable("id") id: ContractFunctionCallRequestId
    ): ResponseEntity<ContractFunctionCallRequestResponse> {
        val contractFunctionCallRequest = contractFunctionCallRequestService.getContractFunctionCallRequest(id)
        return ResponseEntity.ok(ContractFunctionCallRequestResponse(contractFunctionCallRequest))
    }

    @ApiReadLimitedMapping(IdType.PROJECT_ID, "/v1/function-call/by-project/{projectId}")
    fun getContractFunctionCallRequestsByProjectIdAndFilters(
        @PathVariable("projectId") projectId: ProjectId,
        @RequestParam("deployedContractId", required = false) deployedContractId: ContractDeploymentRequestId?,
        @ValidEthAddress @RequestParam("contractAddress", required = false) contractAddress: String?
    ): ResponseEntity<ContractFunctionCallRequestsResponse> {
        val contractFunctionCallRequests = contractFunctionCallRequestService
            .getContractFunctionCallRequestsByProjectIdAndFilters(
                projectId = projectId,
                filters = ContractFunctionCallRequestFilters(
                    deployedContractId = deployedContractId,
                    contractAddress = contractAddress?.let { ContractAddress(it) }
                )
            )
        return ResponseEntity.ok(
            ContractFunctionCallRequestsResponse(
                contractFunctionCallRequests.map { ContractFunctionCallRequestResponse(it) }
            )
        )
    }

    @ApiWriteLimitedMapping(IdType.FUNCTION_CALL_REQUEST_ID, RequestMethod.PUT, "/v1/function-call/{id}")
    fun attachTransactionInfo(
        @PathVariable("id") id: ContractFunctionCallRequestId,
        @Valid @RequestBody requestBody: AttachTransactionInfoRequest
    ) {
        contractFunctionCallRequestService.attachTxInfo(
            id = id,
            txHash = TransactionHash(requestBody.txHash),
            caller = WalletAddress(requestBody.callerAddress)
        )
    }
}
