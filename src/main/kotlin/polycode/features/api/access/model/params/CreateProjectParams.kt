package polycode.features.api.access.model.params

import polycode.util.BaseUrl
import polycode.util.ChainId

data class CreateProjectParams(
    val baseRedirectUrl: BaseUrl,
    val chainId: ChainId,
    val customRpcUrl: String?
)
