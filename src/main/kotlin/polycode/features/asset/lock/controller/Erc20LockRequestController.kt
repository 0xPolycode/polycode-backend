package polycode.features.asset.lock.controller

import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import polycode.config.binding.annotation.ApiKeyBinding
import polycode.config.interceptors.annotation.ApiReadLimitedMapping
import polycode.config.interceptors.annotation.ApiWriteLimitedMapping
import polycode.config.interceptors.annotation.IdType
import polycode.features.api.access.model.result.Project
import polycode.features.asset.lock.model.params.CreateErc20LockRequestParams
import polycode.features.asset.lock.model.request.CreateErc20LockRequest
import polycode.features.asset.lock.model.response.Erc20LockRequestResponse
import polycode.features.asset.lock.model.response.Erc20LockRequestsResponse
import polycode.features.asset.lock.service.Erc20LockRequestService
import polycode.generated.jooq.id.Erc20LockRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.model.request.AttachTransactionInfoRequest
import polycode.util.TransactionHash
import polycode.util.WalletAddress
import javax.validation.Valid

@Validated
@RestController
class Erc20LockRequestController(private val erc20LockRequestService: Erc20LockRequestService) {

    @ApiWriteLimitedMapping(IdType.PROJECT_ID, RequestMethod.POST, "/v1/lock")
    fun createErc20LockRequest(
        @ApiKeyBinding project: Project,
        @Valid @RequestBody requestBody: CreateErc20LockRequest
    ): ResponseEntity<Erc20LockRequestResponse> {
        val params = CreateErc20LockRequestParams(requestBody)
        val createdRequest = erc20LockRequestService.createErc20LockRequest(params, project)
        return ResponseEntity.ok(Erc20LockRequestResponse(createdRequest))
    }

    @ApiReadLimitedMapping(IdType.ERC20_LOCK_REQUEST_ID, "/v1/lock/{id}")
    fun getErc20LockRequest(
        @PathVariable("id") id: Erc20LockRequestId
    ): ResponseEntity<Erc20LockRequestResponse> {
        val lockRequest = erc20LockRequestService.getErc20LockRequest(id)
        return ResponseEntity.ok(Erc20LockRequestResponse(lockRequest))
    }

    @ApiReadLimitedMapping(IdType.PROJECT_ID, "/v1/lock/by-project/{projectId}")
    fun getErc20LockRequestsByProjectId(
        @PathVariable("projectId") projectId: ProjectId
    ): ResponseEntity<Erc20LockRequestsResponse> {
        val lockRequests = erc20LockRequestService.getErc20LockRequestsByProjectId(projectId)
        return ResponseEntity.ok(Erc20LockRequestsResponse(lockRequests.map { Erc20LockRequestResponse(it) }))
    }

    @ApiWriteLimitedMapping(IdType.ERC20_LOCK_REQUEST_ID, RequestMethod.PUT, "/v1/lock/{id}")
    fun attachTransactionInfo(
        @PathVariable("id") id: Erc20LockRequestId,
        @Valid @RequestBody requestBody: AttachTransactionInfoRequest
    ) {
        erc20LockRequestService.attachTxInfo(
            id = id,
            txHash = TransactionHash(requestBody.txHash),
            caller = WalletAddress(requestBody.callerAddress)
        )
    }
}
