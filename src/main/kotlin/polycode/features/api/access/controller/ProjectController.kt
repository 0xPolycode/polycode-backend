package polycode.features.api.access.controller

import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import polycode.config.binding.annotation.ApiKeyBinding
import polycode.config.binding.annotation.UserIdentifierBinding
import polycode.exception.ApiKeyAlreadyExistsException
import polycode.exception.ResourceNotFoundException
import polycode.features.api.access.model.params.CreateProjectParams
import polycode.features.api.access.model.request.CreateProjectRequest
import polycode.features.api.access.model.response.ApiKeyResponse
import polycode.features.api.access.model.response.ProjectResponse
import polycode.features.api.access.model.response.ProjectsResponse
import polycode.features.api.access.model.result.Project
import polycode.features.api.access.model.result.UserIdentifier
import polycode.features.api.access.service.ProjectService
import polycode.features.api.analytics.service.AnalyticsService
import polycode.generated.jooq.id.ProjectId
import polycode.util.BaseUrl
import polycode.util.ChainId
import javax.servlet.http.HttpServletRequest
import javax.validation.Valid

@Validated
@RestController
class ProjectController(
    private val projectService: ProjectService,
    private val analyticsService: AnalyticsService
) {

    @PostMapping("/v1/projects")
    fun createProject(
        @UserIdentifierBinding userIdentifier: UserIdentifier,
        @Valid @RequestBody requestBody: CreateProjectRequest
    ): ResponseEntity<ProjectResponse> {
        val params = CreateProjectParams(
            baseRedirectUrl = BaseUrl(requestBody.baseRedirectUrl),
            chainId = ChainId(requestBody.chainId),
            customRpcUrl = requestBody.customRpcUrl
        )

        val project = projectService.createProject(userIdentifier, params)

        return ResponseEntity.ok(ProjectResponse(project))
    }

    @GetMapping("/v1/projects/by-api-key")
    fun getByApiKey(
        @ApiKeyBinding project: Project
    ): ResponseEntity<ProjectResponse> {
        return ResponseEntity.ok(ProjectResponse(project))
    }

    @GetMapping("/v1/projects/{id}")
    fun getById(
        @UserIdentifierBinding userIdentifier: UserIdentifier,
        @PathVariable id: ProjectId
    ): ResponseEntity<ProjectResponse> {
        val project = projectService.getProjectById(userIdentifier, id)
        return ResponseEntity.ok(ProjectResponse(project))
    }

    @GetMapping("/v1/projects")
    fun getAll(@UserIdentifierBinding userIdentifier: UserIdentifier): ResponseEntity<ProjectsResponse> {
        val projects = projectService.getAllProjectsForUser(userIdentifier)
        return ResponseEntity.ok(ProjectsResponse(projects.map { ProjectResponse(it) }))
    }

    @GetMapping("/v1/projects/{id}/api-key")
    fun getApiKey(
        @UserIdentifierBinding userIdentifier: UserIdentifier,
        @PathVariable id: ProjectId
    ): ResponseEntity<ApiKeyResponse> { // TODO return multiple API keys in the future
        val apiKey = projectService.getProjectApiKeys(userIdentifier, id).firstOrNull()
            ?: throw ResourceNotFoundException("API key not yet generated for provided project ID")
        return ResponseEntity.ok(ApiKeyResponse(apiKey))
    }

    @PostMapping("/v1/projects/{id}/api-key")
    fun createApiKey(
        @UserIdentifierBinding userIdentifier: UserIdentifier,
        @PathVariable id: ProjectId,
        request: HttpServletRequest
    ): ResponseEntity<ApiKeyResponse> { // TODO allow multiple API key creation in the future
        if (projectService.getProjectApiKeys(userIdentifier, id).isNotEmpty()) {
            throw ApiKeyAlreadyExistsException(id)
        }

        val apiKey = projectService.createApiKey(userIdentifier, id)

        ignoreErrors {
            analyticsService.postApiKeyCreatedEvent(
                userIdentifier = userIdentifier,
                projectId = apiKey.projectId,
                origin = request.getHeader("Origin"),
                userAgent = request.getHeader("User-Agent"),
                remoteAddr = request.remoteAddr
            )
        }

        return ResponseEntity.ok(ApiKeyResponse(apiKey))
    }

    private fun ignoreErrors(call: () -> Unit) =
        try {
            call()
        } catch (_: Exception) {
        }
}
