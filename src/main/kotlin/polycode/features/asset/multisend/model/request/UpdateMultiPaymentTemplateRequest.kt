package polycode.features.asset.multisend.model.request

import polycode.config.validation.MaxStringSize
import polycode.config.validation.ValidEthAddress
import polycode.exception.MissingTokenAddressException
import polycode.exception.TokenAddressNotAllowedException
import polycode.util.AssetType
import javax.validation.constraints.NotNull

data class UpdateMultiPaymentTemplateRequest(
    @field:NotNull
    @field:MaxStringSize
    val templateName: String,
    @field:NotNull
    val assetType: AssetType,
    @field:ValidEthAddress
    val tokenAddress: String?,
    @field:NotNull
    val chainId: Long
) {
    init {
        when (assetType) {
            AssetType.NATIVE -> if (tokenAddress != null) throw TokenAddressNotAllowedException()
            AssetType.TOKEN -> if (tokenAddress == null) throw MissingTokenAddressException()
        }
    }
}
