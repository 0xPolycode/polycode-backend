package polycode.features.wallet.authorization.controller

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
import polycode.features.wallet.authorization.model.params.CreateAuthorizationRequestParams
import polycode.features.wallet.authorization.model.request.CreateAuthorizationRequest
import polycode.features.wallet.authorization.model.response.AuthorizationRequestResponse
import polycode.features.wallet.authorization.model.response.AuthorizationRequestsResponse
import polycode.features.wallet.authorization.service.AuthorizationRequestService
import polycode.generated.jooq.id.AuthorizationRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.model.request.AttachSignedMessageRequest
import polycode.util.SignedMessage
import polycode.util.WalletAddress
import javax.validation.Valid

@Validated
@RestController
class AuthorizationRequestController(private val authorizationRequestService: AuthorizationRequestService) {

    @ApiWriteLimitedMapping(IdType.PROJECT_ID, RequestMethod.POST, "/v1/wallet-authorization")
    fun createAuthorizationRequest(
        @ApiKeyBinding project: Project,
        @Valid @RequestBody requestBody: CreateAuthorizationRequest
    ): ResponseEntity<AuthorizationRequestResponse> {
        val params = CreateAuthorizationRequestParams(requestBody)
        val createdRequest = authorizationRequestService.createAuthorizationRequest(params, project)
        return ResponseEntity.ok(AuthorizationRequestResponse(createdRequest))
    }

    @ApiReadLimitedMapping(IdType.AUTHORIZATION_REQUEST_ID, "/v1/wallet-authorization/{id}")
    fun getAuthorizationRequest(
        @PathVariable("id") id: AuthorizationRequestId
    ): ResponseEntity<AuthorizationRequestResponse> {
        val authorizationRequest = authorizationRequestService.getAuthorizationRequest(id)
        return ResponseEntity.ok(AuthorizationRequestResponse(authorizationRequest))
    }

    @ApiReadLimitedMapping(IdType.PROJECT_ID, "/v1/wallet-authorization/by-project/{projectId}")
    fun getAuthorizationRequestsByProjectId(
        @PathVariable("projectId") projectId: ProjectId
    ): ResponseEntity<AuthorizationRequestsResponse> {
        val authorizationRequests = authorizationRequestService.getAuthorizationRequestsByProjectId(projectId)
        return ResponseEntity.ok(
            AuthorizationRequestsResponse(authorizationRequests.map { AuthorizationRequestResponse(it) })
        )
    }

    @ApiWriteLimitedMapping(IdType.AUTHORIZATION_REQUEST_ID, RequestMethod.PUT, "/v1/wallet-authorization/{id}")
    fun attachSignedMessage(
        @PathVariable("id") id: AuthorizationRequestId,
        @Valid @RequestBody requestBody: AttachSignedMessageRequest
    ) {
        authorizationRequestService.attachWalletAddressAndSignedMessage(
            id = id,
            walletAddress = WalletAddress(requestBody.walletAddress),
            signedMessage = SignedMessage(requestBody.signedMessage)
        )
    }
}
