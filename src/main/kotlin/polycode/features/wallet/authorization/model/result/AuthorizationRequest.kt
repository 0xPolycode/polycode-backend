package polycode.features.wallet.authorization.model.result

import com.fasterxml.jackson.databind.JsonNode
import polycode.generated.jooq.id.AuthorizationRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.model.ScreenConfig
import polycode.util.SignedMessage
import polycode.util.UtcDateTime
import polycode.util.WalletAddress

data class AuthorizationRequest(
    val id: AuthorizationRequestId,
    val projectId: ProjectId,
    val redirectUrl: String,
    val messageToSignOverride: String?,
    val storeIndefinitely: Boolean,
    val requestedWalletAddress: WalletAddress?,
    val actualWalletAddress: WalletAddress?,
    val signedMessage: SignedMessage?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig,
    val createdAt: UtcDateTime
) {
    val messageToSign: String
        get() = messageToSignOverride ?: "Authorization message ID to sign: ${id.value}"
}
