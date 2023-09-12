package polycode.features.wallet.login.model.result

import polycode.generated.jooq.id.WalletLoginRequestId
import polycode.util.SignedMessage
import polycode.util.UtcDateTime
import polycode.util.WalletAddress

data class WalletLoginRequest(
    val id: WalletLoginRequestId,
    val walletAddress: WalletAddress,
    val messageToSign: String,
    val signedMessage: SignedMessage?,
    val createdAt: UtcDateTime
)
