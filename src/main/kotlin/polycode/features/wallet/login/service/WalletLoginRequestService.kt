package polycode.features.wallet.login.service

import polycode.config.authentication.JwtAuthToken
import polycode.features.wallet.login.model.params.CreateWalletLoginRequestParams
import polycode.features.wallet.login.model.result.WalletLoginRequest
import polycode.generated.jooq.id.WalletLoginRequestId
import polycode.util.SignedMessage

interface WalletLoginRequestService {
    fun createWalletLoginRequest(params: CreateWalletLoginRequestParams): WalletLoginRequest
    fun attachSignedMessageAndVerifyLogin(id: WalletLoginRequestId, signedMessage: SignedMessage): JwtAuthToken
}
