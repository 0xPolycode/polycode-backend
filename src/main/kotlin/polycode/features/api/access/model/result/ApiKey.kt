package polycode.features.api.access.model.result

import polycode.generated.jooq.id.ApiKeyId
import polycode.generated.jooq.id.ProjectId
import polycode.util.UtcDateTime

data class ApiKey(
    val id: ApiKeyId,
    val projectId: ProjectId,
    val apiKey: String,
    val createdAt: UtcDateTime
)
