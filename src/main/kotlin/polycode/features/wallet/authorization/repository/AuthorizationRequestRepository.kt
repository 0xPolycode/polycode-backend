package polycode.features.wallet.authorization.repository

import polycode.features.wallet.authorization.model.params.StoreAuthorizationRequestParams
import polycode.features.wallet.authorization.model.result.AuthorizationRequest
import polycode.generated.jooq.id.AuthorizationRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.util.SignedMessage
import polycode.util.WalletAddress

interface AuthorizationRequestRepository {
    fun store(params: StoreAuthorizationRequestParams): AuthorizationRequest
    fun delete(id: AuthorizationRequestId)
    fun getById(id: AuthorizationRequestId): AuthorizationRequest?
    fun getAllByProjectId(projectId: ProjectId): List<AuthorizationRequest>
    fun setSignedMessage(
        id: AuthorizationRequestId,
        walletAddress: WalletAddress,
        signedMessage: SignedMessage
    ): Boolean
}
