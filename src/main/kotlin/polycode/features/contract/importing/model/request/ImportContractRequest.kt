package polycode.features.contract.importing.model.request

import com.fasterxml.jackson.databind.JsonNode
import polycode.config.validation.MaxJsonNodeChars
import polycode.config.validation.MaxStringSize
import polycode.config.validation.ValidAlias
import polycode.config.validation.ValidEthAddress
import polycode.model.ScreenConfig
import javax.validation.Valid
import javax.validation.constraints.NotNull

data class ImportContractRequest(
    @field:NotNull
    @field:ValidAlias
    val alias: String,
    @field:MaxStringSize
    val contractId: String?,
    @field:NotNull
    @field:ValidEthAddress
    val contractAddress: String,
    @field:MaxStringSize
    val redirectUrl: String?,
    @field:MaxJsonNodeChars
    val arbitraryData: JsonNode?,
    @field:Valid
    val screenConfig: ScreenConfig?
)
