package polycode.features.asset.multisend.controller

import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import polycode.config.binding.annotation.ApiKeyBinding
import polycode.config.interceptors.annotation.ApiReadLimitedMapping
import polycode.config.interceptors.annotation.ApiWriteLimitedMapping
import polycode.config.interceptors.annotation.IdType
import polycode.config.validation.ValidEthAddress
import polycode.features.api.access.model.result.Project
import polycode.features.asset.multisend.model.params.CreateAssetMultiSendRequestParams
import polycode.features.asset.multisend.model.request.CreateAssetMultiSendRequest
import polycode.features.asset.multisend.model.response.AssetMultiSendRequestResponse
import polycode.features.asset.multisend.model.response.AssetMultiSendRequestsResponse
import polycode.features.asset.multisend.service.AssetMultiSendRequestService
import polycode.generated.jooq.id.AssetMultiSendRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.model.request.AttachTransactionInfoRequest
import polycode.util.TransactionHash
import polycode.util.WalletAddress
import javax.validation.Valid

@Validated
@RestController
class AssetMultiSendRequestController(private val assetMultiSendRequestService: AssetMultiSendRequestService) {

    @ApiWriteLimitedMapping(IdType.PROJECT_ID, RequestMethod.POST, "/v1/multi-send")
    fun createAssetMultiSendRequest(
        @ApiKeyBinding project: Project,
        @Valid @RequestBody requestBody: CreateAssetMultiSendRequest
    ): ResponseEntity<AssetMultiSendRequestResponse> {
        val params = CreateAssetMultiSendRequestParams(requestBody)
        val createdRequest = assetMultiSendRequestService.createAssetMultiSendRequest(params, project)
        return ResponseEntity.ok(AssetMultiSendRequestResponse(createdRequest))
    }

    @ApiReadLimitedMapping(IdType.ASSET_MULTI_SEND_REQUEST_ID, "/v1/multi-send/{id}")
    fun getAssetMultiSendRequest(
        @PathVariable("id") id: AssetMultiSendRequestId
    ): ResponseEntity<AssetMultiSendRequestResponse> {
        val request = assetMultiSendRequestService.getAssetMultiSendRequest(id)
        return ResponseEntity.ok(AssetMultiSendRequestResponse(request))
    }

    @ApiReadLimitedMapping(IdType.PROJECT_ID, "/v1/multi-send/by-project/{projectId}")
    fun getAssetMultiSendRequestsByProjectId(
        @PathVariable("projectId") projectId: ProjectId
    ): ResponseEntity<AssetMultiSendRequestsResponse> {
        val requests = assetMultiSendRequestService.getAssetMultiSendRequestsByProjectId(projectId)
        return ResponseEntity.ok(AssetMultiSendRequestsResponse(requests.map { AssetMultiSendRequestResponse(it) }))
    }

    @GetMapping("/v1/multi-send/by-sender/{sender}")
    fun getAssetMultiSendRequestsBySender(
        @ValidEthAddress @PathVariable("sender") sender: String
    ): ResponseEntity<AssetMultiSendRequestsResponse> {
        val requests = assetMultiSendRequestService.getAssetMultiSendRequestsBySender(WalletAddress(sender))
        return ResponseEntity.ok(AssetMultiSendRequestsResponse(requests.map { AssetMultiSendRequestResponse(it) }))
    }

    @ApiWriteLimitedMapping(IdType.ASSET_MULTI_SEND_REQUEST_ID, RequestMethod.PUT, "/v1/multi-send/{id}/approve")
    fun attachApproveTransactionInfo(
        @PathVariable("id") id: AssetMultiSendRequestId,
        @Valid @RequestBody requestBody: AttachTransactionInfoRequest
    ) {
        assetMultiSendRequestService.attachApproveTxInfo(
            id = id,
            txHash = TransactionHash(requestBody.txHash),
            caller = WalletAddress(requestBody.callerAddress)
        )
    }

    @ApiWriteLimitedMapping(IdType.ASSET_MULTI_SEND_REQUEST_ID, RequestMethod.PUT, "/v1/multi-send/{id}/disperse")
    fun attachDisperseTransactionInfo(
        @PathVariable("id") id: AssetMultiSendRequestId,
        @Valid @RequestBody requestBody: AttachTransactionInfoRequest
    ) {
        assetMultiSendRequestService.attachDisperseTxInfo(
            id = id,
            txHash = TransactionHash(requestBody.txHash),
            caller = WalletAddress(requestBody.callerAddress)
        )
    }
}
