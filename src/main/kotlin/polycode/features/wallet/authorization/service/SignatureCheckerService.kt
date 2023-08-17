package polycode.features.wallet.authorization.service

import polycode.util.SignedMessage
import polycode.util.WalletAddress

interface SignatureCheckerService {
    fun signatureMatches(message: String, signedMessage: SignedMessage, signer: WalletAddress): Boolean
}
