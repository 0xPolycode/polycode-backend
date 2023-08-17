package polycode.service

import org.junit.jupiter.api.Test
import polycode.TestBase
import polycode.features.wallet.authorization.service.EthereumSignatureCheckerService
import polycode.util.SignedMessage
import polycode.util.WalletAddress

class EthereumSignatureCheckerServiceTest : TestBase() {

    companion object {
        const val MESSAGE = "Verification message ID to sign: 7d86b0ac-a9a6-40fc-ac6d-2a29ca687f73"

        val METAMASK_WALLET = WalletAddress("0x865f603F42ca1231e5B5F90e15663b0FE19F0b21")
        val METAMASK_SIGNATURE = SignedMessage(
            "0xfc90c8aa9f2164234b8826144d8ecfc287b5d7c168d0e9d284baf76dbef55c4c5761cf46e34b7cdb72cc97f1fb1c19f315ee7a" +
                "430dd6111fa6c693b41c96c5501c"
        )

        val LEDGER_WALLET = WalletAddress("0xA105df45DBa0ace0A44b0A643deeD71F9A209D7e")
        val LEDGER_SIGNATURE = SignedMessage(
            "0xab8d88f0e5e75179b6a48e22253d0505de92339f2fe1cf15738fcbb33c0a03f546dbf922c776b10d446ad1a604502c61f5a9a9" +
                "8852ee1a3fbe7e23017706084501"
        )

        // also signed using Metamask, but using another address
        val OTHER_SIGNATURE = SignedMessage(
            "0x653d99ce15acbfe1cb0c967ecac59781a6d5192b2c50d3ae89c8fdc14c60e37e24704719abb1d34572335861ff48d0d22adaf5" +
                "145339de09afc8820d82fba77b1b"
        )
        val TOO_SHORT_SIGNATURE = SignedMessage("0x")
        val INVALID_SIGNATURE = SignedMessage(
            "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
                "xxxxxxxxxxxxxxxxxxxxxxxxxxxx"
        )
    }

    @Test
    fun mustReturnTrueForMatchingMetamaskSignature() {
        val service = EthereumSignatureCheckerService()

        verify("signature matches") {
            expectThat(service.signatureMatches(MESSAGE, METAMASK_SIGNATURE, METAMASK_WALLET))
                .isTrue()
        }
    }

    @Test
    fun mustReturnTrueForMatchingLedgerSignature() {
        val service = EthereumSignatureCheckerService()

        verify("signature matches") {
            expectThat(service.signatureMatches(MESSAGE, LEDGER_SIGNATURE, LEDGER_WALLET))
                .isTrue()
        }
    }

    @Test
    fun mustReturnFalseForMismatchingSignature() {
        val service = EthereumSignatureCheckerService()

        verify("signature does not match") {
            expectThat(service.signatureMatches(MESSAGE, OTHER_SIGNATURE, METAMASK_WALLET))
                .isFalse()
        }
    }

    @Test
    fun mustReturnFalseWhenSignatureIsTooShort() {
        val service = EthereumSignatureCheckerService()

        verify("signature does not match") {
            expectThat(service.signatureMatches(MESSAGE, TOO_SHORT_SIGNATURE, METAMASK_WALLET))
                .isFalse()
        }
    }

    @Test
    fun mustReturnFalseWhenSignatureIsInvalid() {
        val service = EthereumSignatureCheckerService()

        verify("signature does not match") {
            expectThat(service.signatureMatches(MESSAGE, INVALID_SIGNATURE, METAMASK_WALLET))
                .isFalse()
        }
    }
}
