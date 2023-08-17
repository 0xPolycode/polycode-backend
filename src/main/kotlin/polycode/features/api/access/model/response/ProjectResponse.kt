package polycode.features.api.access.model.response

import polycode.features.api.access.model.result.Project
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import java.time.OffsetDateTime

data class ProjectResponse(
    val id: ProjectId,
    val ownerId: UserId,
    val baseRedirectUrl: String,
    val chainId: Long,
    val customRpcUrl: String?,
    val createdAt: OffsetDateTime
) {
    constructor(project: Project) : this(
        id = project.id,
        ownerId = project.ownerId,
        baseRedirectUrl = project.baseRedirectUrl.value,
        chainId = project.chainId.value,
        customRpcUrl = project.customRpcUrl,
        createdAt = project.createdAt.value
    )
}
