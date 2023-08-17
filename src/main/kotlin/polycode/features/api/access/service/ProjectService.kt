package polycode.features.api.access.service

import polycode.features.api.access.model.params.CreateProjectParams
import polycode.features.api.access.model.result.ApiKey
import polycode.features.api.access.model.result.Project
import polycode.features.api.access.model.result.UserIdentifier
import polycode.generated.jooq.id.ProjectId

interface ProjectService {
    fun createProject(userIdentifier: UserIdentifier, params: CreateProjectParams): Project
    fun getProjectById(userIdentifier: UserIdentifier, id: ProjectId): Project
    fun getAllProjectsForUser(userIdentifier: UserIdentifier): List<Project>
    fun getProjectApiKeys(userIdentifier: UserIdentifier, projectId: ProjectId): List<ApiKey>
    fun createApiKey(userIdentifier: UserIdentifier, projectId: ProjectId): ApiKey
}
