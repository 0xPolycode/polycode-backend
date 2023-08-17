package polycode.features.asset.send.controller

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
import polycode.features.asset.send.model.params.CreateAssetSendRequestParams
import polycode.features.asset.send.model.request.CreateAssetSendRequest
import polycode.features.asset.send.model.response.AssetSendRequestResponse
import polycode.features.asset.send.model.response.AssetSendRequestsResponse
import polycode.features.asset.send.service.AssetSendRequestService
import polycode.generated.jooq.id.AssetSendRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.model.request.AttachTransactionInfoRequest
import polycode.util.TransactionHash
import polycode.util.WalletAddress
import javax.validation.Valid

@Validated
@RestController
class AssetSendRequestController(private val assetSendRequestService: AssetSendRequestService) {

    @ApiWriteLimitedMapping(IdType.PROJECT_ID, RequestMethod.POST, "/v1/send")
    fun createAssetSendRequest(
        @ApiKeyBinding project: Project,
        @Valid @RequestBody requestBody: CreateAssetSendRequest
    ): ResponseEntity<AssetSendRequestResponse> {
        val params = CreateAssetSendRequestParams(requestBody)
        val createdRequest = assetSendRequestService.createAssetSendRequest(params, project)
        return ResponseEntity.ok(AssetSendRequestResponse(createdRequest))
    }

    @ApiReadLimitedMapping(IdType.ASSET_SEND_REQUEST_ID, "/v1/send/{id}")
    fun getAssetSendRequest(
        @PathVariable("id") id: AssetSendRequestId
    ): ResponseEntity<AssetSendRequestResponse> {
        val sendRequest = assetSendRequestService.getAssetSendRequest(id)
        return ResponseEntity.ok(AssetSendRequestResponse(sendRequest))
    }

    @ApiReadLimitedMapping(IdType.PROJECT_ID, "/v1/send/by-project/{projectId}")
    fun getAssetSendRequestsByProjectId(
        @PathVariable("projectId") projectId: ProjectId
    ): ResponseEntity<AssetSendRequestsResponse> {
        val sendRequests = assetSendRequestService.getAssetSendRequestsByProjectId(projectId)
        return ResponseEntity.ok(AssetSendRequestsResponse(sendRequests.map { AssetSendRequestResponse(it) }))
    }

    @GetMapping("/v1/send/by-sender/{sender}")
    fun getAssetSendRequestsBySender(
        @ValidEthAddress @PathVariable("sender") sender: String
    ): ResponseEntity<AssetSendRequestsResponse> {
        val sendRequests = assetSendRequestService.getAssetSendRequestsBySender(WalletAddress(sender))
        return ResponseEntity.ok(AssetSendRequestsResponse(sendRequests.map { AssetSendRequestResponse(it) }))
    }

    @GetMapping("/v1/send/by-recipient/{recipient}")
    fun getAssetSendRequestsByRecipient(
        @ValidEthAddress @PathVariable("recipient") recipient: String
    ): ResponseEntity<AssetSendRequestsResponse> {
        val sendRequests = assetSendRequestService.getAssetSendRequestsByRecipient(WalletAddress(recipient))
        return ResponseEntity.ok(AssetSendRequestsResponse(sendRequests.map { AssetSendRequestResponse(it) }))
    }

    @ApiWriteLimitedMapping(IdType.ASSET_SEND_REQUEST_ID, RequestMethod.PUT, "/v1/send/{id}")
    fun attachTransactionInfo(
        @PathVariable("id") id: AssetSendRequestId,
        @Valid @RequestBody requestBody: AttachTransactionInfoRequest
    ) {
        assetSendRequestService.attachTxInfo(
            id = id,
            txHash = TransactionHash(requestBody.txHash),
            caller = WalletAddress(requestBody.callerAddress)
        )
    }
}
