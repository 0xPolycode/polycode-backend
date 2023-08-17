package polycode.features.api.usage.controller

import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import polycode.config.binding.annotation.ApiKeyBinding
import polycode.config.binding.annotation.UserIdentifierBinding
import polycode.features.api.access.model.result.Project
import polycode.features.api.access.model.result.UserIdentifier
import polycode.features.api.usage.model.response.ApiUsagePeriodResponse
import polycode.features.api.usage.repository.ApiRateLimitRepository
import polycode.service.UtcDateTimeProvider

@Validated
@RestController
class ApiUsageController(
    private val apiRateLimitRepository: ApiRateLimitRepository,
    private val utcDateTimeProvider: UtcDateTimeProvider
) {

    @GetMapping("/v1/api-usage")
    fun getCurrentApiUsageInfoForUser(
        @UserIdentifierBinding userIdentifier: UserIdentifier
    ): ResponseEntity<ApiUsagePeriodResponse> {
        val usage = apiRateLimitRepository.getCurrentApiUsagePeriod(
            userId = userIdentifier.id,
            currentTime = utcDateTimeProvider.getUtcDateTime()
        )
        return ResponseEntity.ok(ApiUsagePeriodResponse(usage))
    }

    @GetMapping("/v1/api-usage/by-api-key")
    fun getCurrentApiUsageInfoForApiKey(
        @ApiKeyBinding project: Project
    ): ResponseEntity<ApiUsagePeriodResponse> {
        val usage = apiRateLimitRepository.getCurrentApiUsagePeriod(
            userId = project.ownerId,
            currentTime = utcDateTimeProvider.getUtcDateTime()
        )
        return ResponseEntity.ok(ApiUsagePeriodResponse(usage))
    }
}
