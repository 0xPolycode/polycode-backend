package polycode.features.wallet.authorization.model.request

import com.fasterxml.jackson.databind.JsonNode
import polycode.config.validation.MaxJsonNodeChars
import polycode.config.validation.MaxStringSize
import polycode.config.validation.ValidEthAddress
import polycode.model.ScreenConfig
import javax.validation.Valid

data class CreateAuthorizationRequest(
    @field:ValidEthAddress
    val walletAddress: String?,
    @field:MaxStringSize
    val redirectUrl: String?,
    @field:MaxStringSize
    val messageToSign: String?,
    val storeIndefinitely: Boolean?,
    @field:MaxJsonNodeChars
    val arbitraryData: JsonNode?,
    @field:Valid
    val screenConfig: ScreenConfig?
)
