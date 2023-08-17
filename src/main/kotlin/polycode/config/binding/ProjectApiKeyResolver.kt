package polycode.config.binding

import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import polycode.config.CustomHeaders
import polycode.config.binding.annotation.ApiKeyBinding
import polycode.exception.NonExistentApiKeyException
import polycode.features.api.access.model.result.Project
import polycode.features.api.access.repository.ApiKeyRepository
import polycode.features.api.access.repository.ProjectRepository
import javax.servlet.http.HttpServletRequest

class ProjectApiKeyResolver(
    private val apiKeyRepository: ApiKeyRepository,
    private val projectRepository: ProjectRepository
) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        return parameter.parameterType == Project::class.java &&
            parameter.hasParameterAnnotation(ApiKeyBinding::class.java)
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        nativeWebRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?
    ): Project {
        val httpServletRequest = nativeWebRequest.getNativeRequest(HttpServletRequest::class.java)
        val apiKey = httpServletRequest?.getHeader(CustomHeaders.API_KEY_HEADER)
            ?.let { apiKeyRepository.getByValue(it) }
            ?: throw NonExistentApiKeyException()
        return projectRepository.getById(apiKey.projectId)!! // non-null enforced by foreign key constraint in DB
    }
}
