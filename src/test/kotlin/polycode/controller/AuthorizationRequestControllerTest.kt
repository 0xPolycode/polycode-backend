package polycode.controller

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import polycode.JsonSchemaDocumentation
import polycode.TestBase
import polycode.TestData
import polycode.features.api.access.model.result.Project
import polycode.features.wallet.authorization.controller.AuthorizationRequestController
import polycode.features.wallet.authorization.model.params.CreateAuthorizationRequestParams
import polycode.features.wallet.authorization.model.request.CreateAuthorizationRequest
import polycode.features.wallet.authorization.model.response.AuthorizationRequestResponse
import polycode.features.wallet.authorization.model.response.AuthorizationRequestsResponse
import polycode.features.wallet.authorization.model.result.AuthorizationRequest
import polycode.features.wallet.authorization.service.AuthorizationRequestService
import polycode.generated.jooq.id.AuthorizationRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import polycode.model.ScreenConfig
import polycode.model.request.AttachSignedMessageRequest
import polycode.util.BaseUrl
import polycode.util.ChainId
import polycode.util.SignedMessage
import polycode.util.Status
import polycode.util.WalletAddress
import polycode.util.WithStatus
import java.util.UUID

class AuthorizationRequestControllerTest : TestBase() {

    @Test
    fun mustCorrectlyCreateAuthorizationRequest() {
        val params = CreateAuthorizationRequestParams(
            redirectUrl = "redirect-url",
            requestedWalletAddress = WalletAddress("b"),
            messageToSign = "message-to-sign-override",
            storeIndefinitely = true,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            )
        )
        val result = AuthorizationRequest(
            id = AuthorizationRequestId(UUID.randomUUID()),
            projectId = ProjectId(UUID.randomUUID()),
            redirectUrl = params.redirectUrl!!,
            messageToSignOverride = params.messageToSign,
            storeIndefinitely = params.storeIndefinitely,
            requestedWalletAddress = params.requestedWalletAddress,
            actualWalletAddress = null,
            signedMessage = null,
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = params.screenConfig,
            createdAt = TestData.TIMESTAMP
        )
        val project = Project(
            id = result.projectId,
            ownerId = UserId(UUID.randomUUID()),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = ChainId(1337L),
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
        val service = mock<AuthorizationRequestService>()

        suppose("authorization request will be created") {
            call(service.createAuthorizationRequest(params, project))
                .willReturn(result)
        }

        val controller = AuthorizationRequestController(service)

        verify("controller returns correct response") {
            val request = CreateAuthorizationRequest(
                redirectUrl = params.redirectUrl,
                messageToSign = params.messageToSign,
                storeIndefinitely = params.storeIndefinitely,
                walletAddress = params.requestedWalletAddress?.rawValue,
                arbitraryData = params.arbitraryData,
                screenConfig = params.screenConfig
            )
            val response = controller.createAuthorizationRequest(project, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        AuthorizationRequestResponse(
                            id = result.id,
                            projectId = project.id,
                            status = Status.PENDING,
                            redirectUrl = result.redirectUrl,
                            walletAddress = result.requestedWalletAddress?.rawValue,
                            arbitraryData = result.arbitraryData,
                            screenConfig = result.screenConfig.orEmpty(),
                            messageToSign = result.messageToSign,
                            signedMessage = result.signedMessage?.value,
                            createdAt = TestData.TIMESTAMP.value
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchAuthorizationRequest() {
        val id = AuthorizationRequestId(UUID.randomUUID())
        val service = mock<AuthorizationRequestService>()
        val result = WithStatus(
            value = AuthorizationRequest(
                id = id,
                projectId = ProjectId(UUID.randomUUID()),
                redirectUrl = "redirect-url",
                messageToSignOverride = "message-to-sign-override",
                storeIndefinitely = true,
                requestedWalletAddress = WalletAddress("def"),
                actualWalletAddress = WalletAddress("def"),
                arbitraryData = TestData.EMPTY_JSON_OBJECT,
                screenConfig = ScreenConfig(
                    beforeActionMessage = "before-action-message",
                    afterActionMessage = "after-action-message"
                ),
                signedMessage = SignedMessage("signed-message"),
                createdAt = TestData.TIMESTAMP
            ),
            status = Status.SUCCESS
        )

        suppose("some authorization request will be fetched") {
            call(service.getAuthorizationRequest(id))
                .willReturn(result)
        }

        val controller = AuthorizationRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getAuthorizationRequest(id)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        AuthorizationRequestResponse(
                            id = result.value.id,
                            projectId = result.value.projectId,
                            status = result.status,
                            redirectUrl = result.value.redirectUrl,
                            walletAddress = result.value.requestedWalletAddress?.rawValue,
                            arbitraryData = result.value.arbitraryData,
                            screenConfig = result.value.screenConfig,
                            messageToSign = result.value.messageToSign,
                            signedMessage = result.value.signedMessage?.value,
                            createdAt = result.value.createdAt.value
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchAuthorizationRequestsByProjectId() {
        val id = AuthorizationRequestId(UUID.randomUUID())
        val projectId = ProjectId(UUID.randomUUID())
        val service = mock<AuthorizationRequestService>()
        val result =
            WithStatus(
                value = AuthorizationRequest(
                    id = id,
                    projectId = projectId,
                    redirectUrl = "redirect-url",
                    messageToSignOverride = "message-to-sign-override",
                    storeIndefinitely = true,
                    requestedWalletAddress = WalletAddress("def"),
                    actualWalletAddress = WalletAddress("def"),
                    arbitraryData = TestData.EMPTY_JSON_OBJECT,
                    screenConfig = ScreenConfig(
                        beforeActionMessage = "before-action-message",
                        afterActionMessage = "after-action-message"
                    ),
                    signedMessage = SignedMessage("signed-message"),
                    createdAt = TestData.TIMESTAMP
                ),
                status = Status.SUCCESS
            )

        suppose("some authorization requests will be fetched by project ID") {
            call(service.getAuthorizationRequestsByProjectId(projectId))
                .willReturn(listOf(result))
        }

        val controller = AuthorizationRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getAuthorizationRequestsByProjectId(projectId)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        AuthorizationRequestsResponse(
                            listOf(
                                AuthorizationRequestResponse(
                                    id = result.value.id,
                                    projectId = result.value.projectId,
                                    status = result.status,
                                    redirectUrl = result.value.redirectUrl,
                                    walletAddress = result.value.requestedWalletAddress?.rawValue,
                                    arbitraryData = result.value.arbitraryData,
                                    screenConfig = result.value.screenConfig,
                                    messageToSign = result.value.messageToSign,
                                    signedMessage = result.value.signedMessage?.value,
                                    createdAt = result.value.createdAt.value
                                )
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyAttachSignedMessage() {
        val service = mock<AuthorizationRequestService>()
        val controller = AuthorizationRequestController(service)

        val id = AuthorizationRequestId(UUID.randomUUID())
        val walletAddress = WalletAddress("abc")
        val signedMessage = SignedMessage("signed-message")

        suppose("signed message will be attached") {
            val request = AttachSignedMessageRequest(walletAddress.rawValue, signedMessage.value)
            controller.attachSignedMessage(id, request)
            JsonSchemaDocumentation.createSchema(request.javaClass)
        }

        verify("signed message is correctly attached") {
            expectInteractions(service) {
                once.attachWalletAddressAndSignedMessage(id, walletAddress, signedMessage)
            }
        }
    }
}
