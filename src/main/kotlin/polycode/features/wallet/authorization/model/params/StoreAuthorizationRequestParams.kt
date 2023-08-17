package polycode.features.wallet.authorization.model.params

import com.fasterxml.jackson.databind.JsonNode
import polycode.features.api.access.model.result.Project
import polycode.generated.jooq.id.AuthorizationRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.model.ScreenConfig
import polycode.model.params.ParamsFactory
import polycode.util.UtcDateTime
import polycode.util.WalletAddress
import java.util.UUID

data class StoreAuthorizationRequestParams(
    val id: AuthorizationRequestId,
    val projectId: ProjectId,
    val redirectUrl: String,
    val messageToSignOverride: String?,
    val storeIndefinitely: Boolean,
    val requestedWalletAddress: WalletAddress?,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig,
    val createdAt: UtcDateTime
) {
    companion object : ParamsFactory<CreateAuthorizationRequestParams, StoreAuthorizationRequestParams> {
        private const val PATH = "/request-authorization/\${id}/action"

        override fun fromCreateParams(
            id: UUID,
            params: CreateAuthorizationRequestParams,
            project: Project,
            createdAt: UtcDateTime
        ) = StoreAuthorizationRequestParams(
            id = AuthorizationRequestId(id),
            projectId = project.id,
            redirectUrl = project.createRedirectUrl(params.redirectUrl, id, PATH),
            messageToSignOverride = params.messageToSign,
            storeIndefinitely = params.storeIndefinitely,
            requestedWalletAddress = params.requestedWalletAddress,
            arbitraryData = params.arbitraryData,
            screenConfig = params.screenConfig,
            createdAt = createdAt
        )
    }
}
