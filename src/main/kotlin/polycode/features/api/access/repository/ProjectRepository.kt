package polycode.features.api.access.repository

import polycode.features.api.access.model.result.Project
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId

interface ProjectRepository {
    fun store(project: Project): Project
    fun getById(id: ProjectId): Project?
    fun getAllByOwnerId(ownerId: UserId): List<Project>
}
