package polycode.features.wallet.login.model.response

import polycode.features.wallet.login.model.result.WalletLoginRequest
import polycode.generated.jooq.id.WalletLoginRequestId
import java.time.OffsetDateTime

data class WalletLoginRequestResponse(
    val id: WalletLoginRequestId,
    val walletAddress: String,
    val messageToSign: String,
    val createdAt: OffsetDateTime
) {
    constructor(walletLoginRequest: WalletLoginRequest) : this(
        id = walletLoginRequest.id,
        walletAddress = walletLoginRequest.walletAddress.rawValue,
        messageToSign = walletLoginRequest.messageToSign,
        createdAt = walletLoginRequest.createdAt.value
    )
}
