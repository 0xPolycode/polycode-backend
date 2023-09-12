package polycode.features.wallet.login.model.params

import polycode.generated.jooq.id.WalletLoginRequestId
import polycode.util.UtcDateTime
import polycode.util.WalletAddress
import java.util.UUID

data class StoreWalletLoginRequestParams(
    val id: WalletLoginRequestId,
    val walletAddress: WalletAddress,
    val messageToSign: String,
    val createdAt: UtcDateTime
) {
    companion object {
        fun fromCreateParams(
            id: UUID,
            params: CreateWalletLoginRequestParams,
            createdAt: UtcDateTime
        ) = StoreWalletLoginRequestParams(
            id = WalletLoginRequestId(id),
            walletAddress = params.walletAddress,
            messageToSign = "Sign this message to confirm that you are the owner of the wallet:" +
                " ${params.walletAddress.rawValue}\nID to sign: $id, timestamp: ${createdAt.iso}",
            createdAt = createdAt
        )
    }
}
