package polycode.features.wallet.authorization.service

import org.kethereum.crypto.signedMessageToKey
import org.kethereum.crypto.toAddress
import org.kethereum.model.SignatureData
import org.springframework.stereotype.Service
import polycode.util.SignedMessage
import polycode.util.WalletAddress
import java.math.BigInteger

@Service
class EthereumSignatureCheckerService : SignatureCheckerService {

    companion object {
        private const val EIP_191_MAGIC_BYTE = 0x19.toByte()
        private const val SIGNATURE_LENGTH = 132
        private const val R_START = 2
        private const val R_END = 66
        private const val S_START = 66
        private const val S_END = 130
        private const val V_START = 130
        private const val V_END = 132
        private const val HEX_RADIX = 16
        private val V_OFFSET = BigInteger.valueOf(27L)
    }

    override fun signatureMatches(message: String, signedMessage: SignedMessage, signer: WalletAddress): Boolean {
        val signatureData = getSignatureData(signedMessage.value)
        val eip919 = generateEip191Message(message.toByteArray())
        val publicKey = signatureData?.let { signedMessageToKey(eip919, it) }
        val signatureAddress = publicKey?.let { WalletAddress(it.toAddress().toString()) }

        return signatureAddress == signer
    }

    private fun generateEip191Message(message: ByteArray): ByteArray =
        byteArrayOf(EIP_191_MAGIC_BYTE) + ("Ethereum Signed Message:\n" + message.size).toByteArray() + message

    private fun BigInteger.withVOffset(): BigInteger =
        if (this == BigInteger.ZERO || this == BigInteger.ONE) {
            this + V_OFFSET
        } else {
            this
        }

    private fun getSignatureData(signature: String): SignatureData? =
        if (signature.length != SIGNATURE_LENGTH) null else try {
            val r = signature.substring(R_START, R_END)
            val s = signature.substring(S_START, S_END)
            val v = signature.substring(V_START, V_END)

            SignatureData(
                r = BigInteger(r, HEX_RADIX),
                s = BigInteger(s, HEX_RADIX),
                v = BigInteger(v, HEX_RADIX).withVOffset()
            )
        } catch (ex: NumberFormatException) {
            null
        }
}
