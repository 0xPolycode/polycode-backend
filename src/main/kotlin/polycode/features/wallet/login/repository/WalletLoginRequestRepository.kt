package polycode.features.wallet.login.repository

import polycode.features.wallet.login.model.params.StoreWalletLoginRequestParams
import polycode.features.wallet.login.model.result.WalletLoginRequest
import polycode.generated.jooq.id.WalletLoginRequestId
import polycode.util.SignedMessage

interface WalletLoginRequestRepository {
    fun store(params: StoreWalletLoginRequestParams): WalletLoginRequest
    fun getById(id: WalletLoginRequestId): WalletLoginRequest?
    fun setSignedMessage(id: WalletLoginRequestId, signedMessage: SignedMessage): Boolean
}
