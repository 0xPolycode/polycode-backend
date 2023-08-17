package polycode.features.wallet.authorization.service

import polycode.features.api.access.model.result.Project
import polycode.features.wallet.authorization.model.params.CreateAuthorizationRequestParams
import polycode.features.wallet.authorization.model.result.AuthorizationRequest
import polycode.generated.jooq.id.AuthorizationRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.util.SignedMessage
import polycode.util.WalletAddress
import polycode.util.WithStatus

interface AuthorizationRequestService {
    fun createAuthorizationRequest(params: CreateAuthorizationRequestParams, project: Project): AuthorizationRequest
    fun getAuthorizationRequest(id: AuthorizationRequestId): WithStatus<AuthorizationRequest>
    fun getAuthorizationRequestsByProjectId(projectId: ProjectId): List<WithStatus<AuthorizationRequest>>
    fun attachWalletAddressAndSignedMessage(
        id: AuthorizationRequestId,
        walletAddress: WalletAddress,
        signedMessage: SignedMessage
    )
}
