package polycode.features.api.analytics.service

import com.facebook.ads.sdk.APIContext
import com.facebook.ads.sdk.APIException
import com.facebook.ads.sdk.serverside.CustomData
import com.facebook.ads.sdk.serverside.Event
import com.facebook.ads.sdk.serverside.EventRequest
import com.facebook.ads.sdk.serverside.UserData
import mu.KLogging
import org.springframework.stereotype.Service
import polycode.config.MetaPixelProperties
import polycode.features.api.access.model.result.UserIdentifier
import polycode.generated.jooq.id.ProjectId

@Service
class AnalyticsServiceImpl(
    private val metaPixelProperties: MetaPixelProperties
) : AnalyticsService {

    companion object : KLogging() {
        private const val SECONDS_IN_MILLISECOND = 1_000L
    }

    private val context: APIContext? = createApiContext(metaPixelProperties)

    override fun postApiKeyCreatedEvent(
        userIdentifier: UserIdentifier,
        projectId: ProjectId,
        origin: String?,
        userAgent: String?,
        remoteAddr: String?
    ) {
        if (context === null) {
            logger.warn { "Failed to post 'API Key Created' event to Meta Pixel. API not initialized properly!" }
            return
        }

        val pixelId = metaPixelProperties.pixelId

        if (pixelId === null) {
            logger.warn { "Failed to post 'API Key Created' event to Meta Pixel. Missing pixelId configuration!" }
            return
        }

        logger.info {
            "Posting 'API Key Created' event to Meta Pixel for userIdentifier: $userIdentifier, " +
                "projectId: $projectId, origin: $origin, userAgent: $userAgent, remoteAddr: $remoteAddr"
        }

        val event = Event()
            .eventName("Login")
            .eventTime(System.currentTimeMillis() / SECONDS_IN_MILLISECOND)
            .userData(
                UserData()
                    .externalId(userIdentifier.id.toString())
                    .clientUserAgent(userAgent)
                    .clientIpAddress(remoteAddr)
            )
            .eventSourceUrl(origin)
            .customData(
                CustomData().customProperties(
                    hashMapOf(
                        "wallet" to userIdentifier.userIdentifier,
                        "projectId" to projectId.value.toString()
                    )
                )
            )
        try {
            val eventRequest = EventRequest(pixelId, context).addDataItem(event)
            val response = eventRequest.execute()
            logger.debug { "'API Key Created' event posted successfully to Meta Pixel. Response: $response" }
        } catch (e: APIException) {
            logger.warn { "Failed to post 'API Key Created' event to Meta Pixel. Exception: $e" }
        }
    }

    private fun createApiContext(metaPixelProperties: MetaPixelProperties): APIContext? {
        return metaPixelProperties.accessToken?.let { APIContext(it) }
    }
}
