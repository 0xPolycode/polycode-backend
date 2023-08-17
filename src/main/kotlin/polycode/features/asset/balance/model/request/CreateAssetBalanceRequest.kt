package polycode.features.asset.balance.model.request

import com.fasterxml.jackson.databind.JsonNode
import polycode.config.validation.MaxJsonNodeChars
import polycode.config.validation.MaxStringSize
import polycode.config.validation.ValidEthAddress
import polycode.config.validation.ValidUint256
import polycode.exception.MissingTokenAddressException
import polycode.exception.TokenAddressNotAllowedException
import polycode.model.ScreenConfig
import polycode.util.AssetType
import java.math.BigInteger
import javax.validation.Valid
import javax.validation.constraints.NotNull

data class CreateAssetBalanceRequest(
    @field:MaxStringSize
    val redirectUrl: String?,
    @field:ValidEthAddress
    val tokenAddress: String?,
    @field:NotNull
    val assetType: AssetType,
    @field:ValidUint256
    val blockNumber: BigInteger?,
    @field:ValidEthAddress
    val walletAddress: String?,
    @field:MaxJsonNodeChars
    val arbitraryData: JsonNode?,
    @field:Valid
    val screenConfig: ScreenConfig?
) {
    init {
        when (assetType) {
            AssetType.NATIVE -> if (tokenAddress != null) throw TokenAddressNotAllowedException()
            AssetType.TOKEN -> if (tokenAddress == null) throw MissingTokenAddressException()
        }
    }
}
