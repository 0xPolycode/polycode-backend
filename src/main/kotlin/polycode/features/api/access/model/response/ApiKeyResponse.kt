package polycode.features.api.access.model.response

import polycode.features.api.access.model.result.ApiKey
import polycode.generated.jooq.id.ApiKeyId
import polycode.generated.jooq.id.ProjectId
import java.time.OffsetDateTime

data class ApiKeyResponse(
    val id: ApiKeyId,
    val projectId: ProjectId,
    val apiKey: String,
    val createdAt: OffsetDateTime
) {
    constructor(apiKey: ApiKey) : this(
        id = apiKey.id,
        projectId = apiKey.projectId,
        apiKey = apiKey.apiKey,
        createdAt = apiKey.createdAt.value
    )
}
