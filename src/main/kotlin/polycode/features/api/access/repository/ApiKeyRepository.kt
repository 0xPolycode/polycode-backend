package polycode.features.api.access.repository

import polycode.features.api.access.model.result.ApiKey
import polycode.generated.jooq.id.ApiKeyId
import polycode.generated.jooq.id.ProjectId

interface ApiKeyRepository {
    fun store(apiKey: ApiKey): ApiKey
    fun getById(id: ApiKeyId): ApiKey?
    fun getByValue(value: String): ApiKey?
    fun getAllByProjectId(projectId: ProjectId): List<ApiKey>
    fun exists(apiKey: String): Boolean
}
