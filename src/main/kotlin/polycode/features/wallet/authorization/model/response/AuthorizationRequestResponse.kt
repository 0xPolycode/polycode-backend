package polycode.features.wallet.authorization.model.response

import com.fasterxml.jackson.databind.JsonNode
import polycode.features.wallet.authorization.model.result.AuthorizationRequest
import polycode.generated.jooq.id.AuthorizationRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.model.ScreenConfig
import polycode.util.Status
import polycode.util.WithStatus
import java.time.OffsetDateTime

data class AuthorizationRequestResponse(
    val id: AuthorizationRequestId,
    val projectId: ProjectId,
    val status: Status,
    val redirectUrl: String,
    val walletAddress: String?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig?,
    val messageToSign: String,
    val signedMessage: String?,
    val createdAt: OffsetDateTime
) {
    constructor(balanceRequest: AuthorizationRequest) : this(
        id = balanceRequest.id,
        projectId = balanceRequest.projectId,
        status = Status.PENDING,
        redirectUrl = balanceRequest.redirectUrl,
        walletAddress = balanceRequest.requestedWalletAddress?.rawValue ?: balanceRequest.actualWalletAddress?.rawValue,
        arbitraryData = balanceRequest.arbitraryData,
        screenConfig = balanceRequest.screenConfig.orEmpty(),
        messageToSign = balanceRequest.messageToSign,
        signedMessage = balanceRequest.signedMessage?.value,
        createdAt = balanceRequest.createdAt.value
    )

    constructor(balanceRequest: WithStatus<AuthorizationRequest>) : this(
        id = balanceRequest.value.id,
        projectId = balanceRequest.value.projectId,
        status = balanceRequest.status,
        redirectUrl = balanceRequest.value.redirectUrl,
        walletAddress = balanceRequest.value.requestedWalletAddress?.rawValue
            ?: balanceRequest.value.actualWalletAddress?.rawValue,
        arbitraryData = balanceRequest.value.arbitraryData,
        screenConfig = balanceRequest.value.screenConfig.orEmpty(),
        messageToSign = balanceRequest.value.messageToSign,
        signedMessage = balanceRequest.value.signedMessage?.value,
        createdAt = balanceRequest.value.createdAt.value
    )
}
