package polycode.features.payout.model.request

import polycode.config.validation.MaxStringSize
import polycode.config.validation.ValidEthAddress
import polycode.config.validation.ValidUint256
import java.math.BigInteger
import javax.validation.Valid
import javax.validation.constraints.NotNull

data class CreateAssetSnapshotRequest(
    @field:NotNull
    @field:MaxStringSize
    val name: String,
    @field:NotNull
    @field:ValidEthAddress
    val assetAddress: String,
    @field:NotNull
    @field:ValidUint256
    val payoutBlockNumber: BigInteger,
    @field:NotNull
    @field:Valid
    val ignoredHolderAddresses: Set<@NotNull @ValidEthAddress String>
)
