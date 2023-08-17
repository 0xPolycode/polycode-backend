package polycode.features.api.analytics.service

import polycode.features.api.access.model.result.UserIdentifier
import polycode.generated.jooq.id.ProjectId

interface AnalyticsService {
    fun postApiKeyCreatedEvent(
        userIdentifier: UserIdentifier,
        projectId: ProjectId,
        origin: String?,
        userAgent: String?,
        remoteAddr: String?
    )
}
