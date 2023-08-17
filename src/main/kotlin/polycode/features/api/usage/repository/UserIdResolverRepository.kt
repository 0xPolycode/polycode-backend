package polycode.features.api.usage.repository

import polycode.config.interceptors.annotation.IdType
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import java.util.UUID

interface UserIdResolverRepository {
    fun getByProjectId(projectId: ProjectId): UserId?
    fun getUserId(idType: IdType, id: UUID): UserId?
}
