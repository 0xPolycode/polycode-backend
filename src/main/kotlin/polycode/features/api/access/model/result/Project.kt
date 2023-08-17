package polycode.features.api.access.model.result

import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import polycode.util.BaseUrl
import polycode.util.ChainId
import polycode.util.UtcDateTime
import java.util.UUID

data class Project(
    val id: ProjectId,
    val ownerId: UserId,
    val baseRedirectUrl: BaseUrl,
    val chainId: ChainId,
    val customRpcUrl: String?,
    val createdAt: UtcDateTime
) {
    fun createRedirectUrl(redirectUrl: String?, id: UUID, path: String) =
        (redirectUrl ?: (baseRedirectUrl.value + path)).replace("\${id}", id.toString())
}
