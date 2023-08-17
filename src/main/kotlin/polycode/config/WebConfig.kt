package polycode.config

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.BeanCreationException
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestTemplate
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import polycode.config.binding.ProjectApiKeyResolver
import polycode.config.binding.UserIdentifierResolver
import polycode.config.interceptors.ApiKeyWriteCallInterceptor
import polycode.config.interceptors.CorrelationIdInterceptor
import polycode.config.interceptors.ProjectReadCallInterceptor
import polycode.features.api.access.repository.ApiKeyRepository
import polycode.features.api.access.repository.PolyflowUserRepository
import polycode.features.api.access.repository.ProjectRepository
import polycode.features.api.access.repository.UserIdentifierRepository
import polycode.features.api.usage.repository.ApiRateLimitRepository
import polycode.features.api.usage.repository.UserIdResolverRepository
import polycode.service.UtcDateTimeProvider
import polycode.service.UuidProvider

@Configuration
class WebConfig(
    private val uuidProvider: UuidProvider,
    private val utcDateTimeProvider: UtcDateTimeProvider,
    private val userIdentifierRepository: UserIdentifierRepository,
    private val apiKeyRepository: ApiKeyRepository,
    private val apiRateLimitRepository: ApiRateLimitRepository,
    private val userIdResolverRepository: UserIdResolverRepository,
    private val polyflowUserRepository: PolyflowUserRepository,
    private val projectRepository: ProjectRepository,
    private val objectMapper: ObjectMapper
) : WebMvcConfigurer {

    companion object {
        private const val MISSING_PROPERTY_MESSAGE =
            "application property polycode.contract-manifest-service.base-url is not set"
    }

    @Bean("basicJsonRestTemplate")
    fun basicJsonRestTemplate(): RestTemplate =
        RestTemplateBuilder()
            .additionalMessageConverters(MappingJackson2HttpMessageConverter(objectMapper))
            .build()

    @Bean("externalContractDecompilerServiceRestTemplate")
    fun externalContractDecompilerServiceRestTemplate(
        contractManifestServiceProperties: ContractManifestServiceProperties
    ): RestTemplate =
        RestTemplateBuilder()
            .rootUri(contractManifestServiceProperties.baseUrl ?: throw BeanCreationException(MISSING_PROPERTY_MESSAGE))
            .additionalMessageConverters(MappingJackson2HttpMessageConverter(objectMapper))
            .build()

    @Bean("pinataRestTemplate")
    fun pinataRestTemplate(ipfsProperties: IpfsProperties): RestTemplate =
        RestTemplateBuilder()
            .rootUri(ipfsProperties.url)
            .defaultHeader("pinata_api_key", ipfsProperties.apiKey)
            .defaultHeader("pinata_secret_api_key", ipfsProperties.secretApiKey)
            .additionalMessageConverters(MappingJackson2HttpMessageConverter(objectMapper))
            .build()

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(
            UserIdentifierResolver(
                uuidProvider = uuidProvider,
                utcDateTimeProvider = utcDateTimeProvider,
                userIdentifierRepository = userIdentifierRepository,
                polyflowUserRepository = polyflowUserRepository,
                apiRateLimitRepository = apiRateLimitRepository
            )
        )
        resolvers.add(ProjectApiKeyResolver(apiKeyRepository, projectRepository))
    }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(CorrelationIdInterceptor(uuidProvider))
        registry.addInterceptor(
            ApiKeyWriteCallInterceptor(
                apiKeyRepository = apiKeyRepository,
                apiRateLimitRepository = apiRateLimitRepository,
                userIdResolverRepository = userIdResolverRepository,
                utcDateTimeProvider = utcDateTimeProvider,
                objectMapper = objectMapper
            )
        )
        registry.addInterceptor(
            ProjectReadCallInterceptor(
                apiRateLimitRepository = apiRateLimitRepository,
                userIdResolverRepository = userIdResolverRepository,
                utcDateTimeProvider = utcDateTimeProvider,
                objectMapper = objectMapper
            )
        )
    }
}
