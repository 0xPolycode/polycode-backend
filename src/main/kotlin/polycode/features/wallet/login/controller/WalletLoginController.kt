package polycode.features.wallet.login.controller

import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import polycode.features.wallet.login.model.params.CreateWalletLoginRequestParams
import polycode.features.wallet.login.model.request.CreateWalletLoginRequest
import polycode.features.wallet.login.model.request.WalletLoginSignedMessageRequest
import polycode.features.wallet.login.model.response.JwtTokenResponse
import polycode.features.wallet.login.model.response.WalletLoginRequestResponse
import polycode.features.wallet.login.service.WalletLoginRequestService
import polycode.generated.jooq.id.WalletLoginRequestId
import polycode.util.SignedMessage
import javax.validation.Valid

@Validated
@RestController
class WalletLoginController(private val walletLoginRequestService: WalletLoginRequestService) {

    @PostMapping("/v1/wallet-login")
    fun createWalletLoginRequest(
        @Valid @RequestBody requestBody: CreateWalletLoginRequest
    ): ResponseEntity<WalletLoginRequestResponse> {
        val params = CreateWalletLoginRequestParams(requestBody)
        val createdRequest = walletLoginRequestService.createWalletLoginRequest(params)
        return ResponseEntity.ok(WalletLoginRequestResponse(createdRequest))
    }

    @PutMapping("/v1/wallet-login/verify-message/{id}")
    fun verifyLogin(
        @PathVariable("id") id: WalletLoginRequestId,
        @Valid @RequestBody requestBody: WalletLoginSignedMessageRequest
    ): ResponseEntity<JwtTokenResponse> {
        val token = walletLoginRequestService.attachSignedMessageAndVerifyLogin(
            id = id,
            signedMessage = SignedMessage(requestBody.signedMessage)
        )
        return ResponseEntity.ok(JwtTokenResponse(token))
    }
}
