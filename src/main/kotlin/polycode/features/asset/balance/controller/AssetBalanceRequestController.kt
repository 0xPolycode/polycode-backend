package polycode.features.asset.balance.controller

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
import polycode.features.asset.balance.model.params.CreateAssetBalanceRequestParams
import polycode.features.asset.balance.model.request.CreateAssetBalanceRequest
import polycode.features.asset.balance.model.response.AssetBalanceRequestResponse
import polycode.features.asset.balance.model.response.AssetBalanceRequestsResponse
import polycode.features.asset.balance.service.AssetBalanceRequestService
import polycode.generated.jooq.id.AssetBalanceRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.model.request.AttachSignedMessageRequest
import polycode.util.SignedMessage
import polycode.util.WalletAddress
import javax.validation.Valid

@Validated
@RestController
class AssetBalanceRequestController(private val assetBalanceRequestService: AssetBalanceRequestService) {

    @ApiWriteLimitedMapping(IdType.PROJECT_ID, RequestMethod.POST, "/v1/balance")
    fun createAssetBalanceRequest(
        @ApiKeyBinding project: Project,
        @Valid @RequestBody requestBody: CreateAssetBalanceRequest
    ): ResponseEntity<AssetBalanceRequestResponse> {
        val params = CreateAssetBalanceRequestParams(requestBody)
        val createdRequest = assetBalanceRequestService.createAssetBalanceRequest(params, project)
        return ResponseEntity.ok(AssetBalanceRequestResponse(createdRequest))
    }

    @ApiReadLimitedMapping(IdType.ASSET_BALANCE_REQUEST_ID, "/v1/balance/{id}")
    fun getAssetBalanceRequest(
        @PathVariable("id") id: AssetBalanceRequestId
    ): ResponseEntity<AssetBalanceRequestResponse> {
        val balanceRequest = assetBalanceRequestService.getAssetBalanceRequest(id)
        return ResponseEntity.ok(AssetBalanceRequestResponse(balanceRequest))
    }

    @ApiReadLimitedMapping(IdType.PROJECT_ID, "/v1/balance/by-project/{projectId}")
    fun getAssetBalanceRequestsByProjectId(
        @PathVariable("projectId") projectId: ProjectId
    ): ResponseEntity<AssetBalanceRequestsResponse> {
        val balanceRequests = assetBalanceRequestService.getAssetBalanceRequestsByProjectId(projectId)
        return ResponseEntity.ok(AssetBalanceRequestsResponse(balanceRequests.map { AssetBalanceRequestResponse(it) }))
    }

    @ApiWriteLimitedMapping(IdType.ASSET_BALANCE_REQUEST_ID, RequestMethod.PUT, "/v1/balance/{id}")
    fun attachSignedMessage(
        @PathVariable("id") id: AssetBalanceRequestId,
        @Valid @RequestBody requestBody: AttachSignedMessageRequest
    ) {
        assetBalanceRequestService.attachWalletAddressAndSignedMessage(
            id = id,
            walletAddress = WalletAddress(requestBody.walletAddress),
            signedMessage = SignedMessage(requestBody.signedMessage)
        )
    }
}
