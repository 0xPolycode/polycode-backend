package polycode.controller

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import polycode.JsonSchemaDocumentation
import polycode.TestBase
import polycode.TestData
import polycode.config.authentication.JwtAuthToken
import polycode.features.wallet.login.controller.WalletLoginController
import polycode.features.wallet.login.model.params.CreateWalletLoginRequestParams
import polycode.features.wallet.login.model.request.CreateWalletLoginRequest
import polycode.features.wallet.login.model.request.WalletLoginSignedMessageRequest
import polycode.features.wallet.login.model.response.JwtTokenResponse
import polycode.features.wallet.login.model.response.WalletLoginRequestResponse
import polycode.features.wallet.login.model.result.WalletLoginRequest
import polycode.features.wallet.login.service.WalletLoginRequestService
import polycode.generated.jooq.id.WalletLoginRequestId
import polycode.util.Right
import polycode.util.SignedMessage
import polycode.util.WalletAddress
import java.util.UUID

class WalletLoginControllerTest : TestBase() {

    @Test
    fun mustCorrectlyCreateWalletLoginRequest() {
        val params = CreateWalletLoginRequestParams(
            walletAddress = WalletAddress("a")
        )
        val result = WalletLoginRequest(
            id = WalletLoginRequestId(UUID.randomUUID()),
            walletAddress = params.walletAddress,
            messageToSign = "message-to-sign",
            signedMessage = null,
            createdAt = TestData.TIMESTAMP
        )
        val service = mock<WalletLoginRequestService>()

        suppose("wallet login request will be created") {
            call(service.createWalletLoginRequest(params))
                .willReturn(result)
        }

        val controller = WalletLoginController(service)

        verify("controller returns correct response") {
            val request = CreateWalletLoginRequest(
                walletAddress = params.walletAddress.rawValue
            )
            val response = controller.createWalletLoginRequest(request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        WalletLoginRequestResponse(
                            id = result.id,
                            walletAddress = result.walletAddress.rawValue,
                            messageToSign = result.messageToSign,
                            createdAt = TestData.TIMESTAMP.value
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyVerifyLogin() {
        val id = WalletLoginRequestId(UUID.randomUUID())
        val signedMessage = SignedMessage("signed-message")
        val result = JwtAuthToken(
            token = "token",
            id = Right(WalletAddress("a")),
            email = null,
            validUntil = TestData.TIMESTAMP
        )

        val service = mock<WalletLoginRequestService>()

        suppose("wallet login will be verified") {
            call(service.attachSignedMessageAndVerifyLogin(id, signedMessage))
                .willReturn(result)
        }

        val controller = WalletLoginController(service)

        verify("controller returns correct response") {
            val request = WalletLoginSignedMessageRequest(signedMessage.value)
            val response = controller.verifyLogin(id, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        JwtTokenResponse(result)
                    )
                )
        }
    }
}
