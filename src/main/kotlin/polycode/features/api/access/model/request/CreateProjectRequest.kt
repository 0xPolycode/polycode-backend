package polycode.features.api.access.model.request

import polycode.config.validation.MaxStringSize
import javax.validation.constraints.NotNull

data class CreateProjectRequest(
    @field:NotNull
    @field:MaxStringSize
    val baseRedirectUrl: String,
    @field:NotNull
    val chainId: Long,
    @field:MaxStringSize
    val customRpcUrl: String?
)
