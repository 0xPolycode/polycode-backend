package polycode.features.asset.multisend.model.request

import com.fasterxml.jackson.databind.JsonNode
import polycode.config.validation.MaxJsonNodeChars
import polycode.config.validation.MaxStringSize
import polycode.config.validation.ValidEthAddress
import polycode.exception.MissingTokenAddressException
import polycode.exception.TokenAddressNotAllowedException
import polycode.model.ScreenConfig
import polycode.util.AssetType
import javax.validation.Valid
import javax.validation.constraints.NotNull

data class CreateAssetMultiSendRequest(
    @field:MaxStringSize
    val redirectUrl: String?,
    @field:ValidEthAddress
    val tokenAddress: String?,
    @field:NotNull
    @field:ValidEthAddress
    val disperseContractAddress: String,
    @field:NotNull
    val assetType: AssetType,
    @field:NotNull
    @field:Valid
    val items: List<MultiPaymentTemplateItemRequest>,
    @field:ValidEthAddress
    val senderAddress: String?,
    @field:MaxJsonNodeChars
    val arbitraryData: JsonNode?,
    @field:Valid
    val approveScreenConfig: ScreenConfig?,
    @field:Valid
    val disperseScreenConfig: ScreenConfig?
) {
    init {
        when (assetType) {
            AssetType.NATIVE -> if (tokenAddress != null) throw TokenAddressNotAllowedException()
            AssetType.TOKEN -> if (tokenAddress == null) throw MissingTokenAddressException()
        }
    }
}
