package polycode.features.contract.arbitrarycall.controller

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
import polycode.features.contract.arbitrarycall.model.filters.ContractArbitraryCallRequestFilters
import polycode.features.contract.arbitrarycall.model.params.CreateContractArbitraryCallRequestParams
import polycode.features.contract.arbitrarycall.model.request.CreateContractArbitraryCallRequest
import polycode.features.contract.arbitrarycall.model.response.ContractArbitraryCallRequestResponse
import polycode.features.contract.arbitrarycall.model.response.ContractArbitraryCallRequestsResponse
import polycode.features.contract.arbitrarycall.service.ContractArbitraryCallRequestService
import polycode.generated.jooq.id.ContractArbitraryCallRequestId
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.model.request.AttachTransactionInfoRequest
import polycode.util.ContractAddress
import polycode.util.TransactionHash
import polycode.util.WalletAddress
import javax.validation.Valid

@Validated
@RestController
class ContractArbitraryCallRequestController(
    private val contractArbitraryCallRequestService: ContractArbitraryCallRequestService
) {

    @ApiWriteLimitedMapping(IdType.PROJECT_ID, RequestMethod.POST, "/v1/arbitrary-call")
    fun createContractArbitraryCallRequest(
        @ApiKeyBinding project: Project,
        @Valid @RequestBody requestBody: CreateContractArbitraryCallRequest
    ): ResponseEntity<ContractArbitraryCallRequestResponse> {
        val params = CreateContractArbitraryCallRequestParams(requestBody)
        val createdRequest = contractArbitraryCallRequestService.createContractArbitraryCallRequest(params, project)
        return ResponseEntity.ok(ContractArbitraryCallRequestResponse(createdRequest))
    }

    @ApiReadLimitedMapping(IdType.ARBITRARY_CALL_REQUEST_ID, "/v1/arbitrary-call/{id}")
    fun getContractArbitraryCallRequest(
        @PathVariable("id") id: ContractArbitraryCallRequestId
    ): ResponseEntity<ContractArbitraryCallRequestResponse> {
        val contractArbitraryCallRequest = contractArbitraryCallRequestService.getContractArbitraryCallRequest(id)
        return ResponseEntity.ok(ContractArbitraryCallRequestResponse(contractArbitraryCallRequest))
    }

    @ApiReadLimitedMapping(IdType.PROJECT_ID, "/v1/arbitrary-call/by-project/{projectId}")
    fun getContractArbitraryCallRequestsByProjectIdAndFilters(
        @PathVariable("projectId") projectId: ProjectId,
        @RequestParam("deployedContractId", required = false) deployedContractId: ContractDeploymentRequestId?,
        @ValidEthAddress @RequestParam("contractAddress", required = false) contractAddress: String?
    ): ResponseEntity<ContractArbitraryCallRequestsResponse> {
        val contractArbitraryCallRequests = contractArbitraryCallRequestService
            .getContractArbitraryCallRequestsByProjectIdAndFilters(
                projectId = projectId,
                filters = ContractArbitraryCallRequestFilters(
                    deployedContractId = deployedContractId,
                    contractAddress = contractAddress?.let { ContractAddress(it) }
                )
            )
        return ResponseEntity.ok(
            ContractArbitraryCallRequestsResponse(
                contractArbitraryCallRequests.map { ContractArbitraryCallRequestResponse(it) }
            )
        )
    }

    @ApiWriteLimitedMapping(IdType.ARBITRARY_CALL_REQUEST_ID, RequestMethod.PUT, "/v1/arbitrary-call/{id}")
    fun attachTransactionInfo(
        @PathVariable("id") id: ContractArbitraryCallRequestId,
        @Valid @RequestBody requestBody: AttachTransactionInfoRequest
    ) {
        contractArbitraryCallRequestService.attachTxInfo(
            id = id,
            txHash = TransactionHash(requestBody.txHash),
            caller = WalletAddress(requestBody.callerAddress)
        )
    }
}
