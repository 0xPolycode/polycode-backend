package polycode.features.api.access.service

import mu.KLogging
import org.springframework.stereotype.Service
import polycode.exception.ResourceNotFoundException
import polycode.features.api.access.model.params.CreateProjectParams
import polycode.features.api.access.model.result.ApiKey
import polycode.features.api.access.model.result.Project
import polycode.features.api.access.model.result.UserIdentifier
import polycode.features.api.access.repository.ApiKeyRepository
import polycode.features.api.access.repository.ProjectRepository
import polycode.generated.jooq.id.ApiKeyId
import polycode.generated.jooq.id.ProjectId
import polycode.service.RandomProvider
import polycode.service.UtcDateTimeProvider
import polycode.service.UuidProvider
import java.util.Base64

@Service
class ProjectServiceImpl(
    private val uuidProvider: UuidProvider,
    private val utcDateTimeProvider: UtcDateTimeProvider,
    private val randomProvider: RandomProvider,
    private val projectRepository: ProjectRepository,
    private val apiKeyRepository: ApiKeyRepository
) : ProjectService {

    companion object : KLogging() {
        private const val API_KEY_BYTES = 33
        private const val API_KEY_PREFIX_LENGTH = 5
    }

    override fun createProject(userIdentifier: UserIdentifier, params: CreateProjectParams): Project {
        logger.info { "Creating project, userIdentifier: $userIdentifier, params: $params" }

        val project = Project(
            id = uuidProvider.getUuid(ProjectId),
            ownerId = userIdentifier.id,
            baseRedirectUrl = params.baseRedirectUrl,
            chainId = params.chainId,
            customRpcUrl = params.customRpcUrl,
            createdAt = utcDateTimeProvider.getUtcDateTime()
        )

        return projectRepository.store(project)
    }

    override fun getProjectById(userIdentifier: UserIdentifier, id: ProjectId): Project {
        logger.debug {
            "Fetching project by ID, userIdentifier: $userIdentifier, id: $id"
        }

        return projectRepository.getById(id)
            ?.takeIf { it.ownerId == userIdentifier.id }
            ?: throw ResourceNotFoundException("Project not found for ID: $id")
    }

    override fun getAllProjectsForUser(userIdentifier: UserIdentifier): List<Project> {
        logger.debug { "Fetch all projects for user, userIdentifier: $userIdentifier" }
        return projectRepository.getAllByOwnerId(userIdentifier.id)
    }

    override fun getProjectApiKeys(userIdentifier: UserIdentifier, projectId: ProjectId): List<ApiKey> {
        logger.debug { "Fetch API keys for project, userIdentifier: $userIdentifier, projectId: $projectId" }
        val project = getProjectById(userIdentifier, projectId)
        return apiKeyRepository.getAllByProjectId(project.id)
    }

    override fun createApiKey(userIdentifier: UserIdentifier, projectId: ProjectId): ApiKey {
        logger.info { "Creating API key for project, userIdentifier: $userIdentifier, projectId: $projectId" }
        val project = getProjectById(userIdentifier, projectId)
        val apiKeyBytes = randomProvider.getBytes(API_KEY_BYTES)
        val encodedApiKey = Base64.getEncoder().encodeToString(apiKeyBytes)
        val apiKey = "${encodedApiKey.take(API_KEY_PREFIX_LENGTH)}.${encodedApiKey.drop(API_KEY_PREFIX_LENGTH)}"

        return apiKeyRepository.store(
            ApiKey(
                id = uuidProvider.getUuid(ApiKeyId),
                projectId = project.id,
                apiKey = apiKey,
                createdAt = utcDateTimeProvider.getUtcDateTime()
            )
        )
    }
}
