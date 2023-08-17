package polycode.features.wallet.authorization.repository

import mu.KLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import polycode.features.wallet.authorization.model.params.StoreAuthorizationRequestParams
import polycode.features.wallet.authorization.model.result.AuthorizationRequest
import polycode.generated.jooq.id.AuthorizationRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.tables.AuthorizationRequestTable
import polycode.generated.jooq.tables.records.AuthorizationRequestRecord
import polycode.model.ScreenConfig
import polycode.util.SignedMessage
import polycode.util.WalletAddress

@Repository
class JooqAuthorizationRequestRepository(private val dslContext: DSLContext) : AuthorizationRequestRepository {

    companion object : KLogging()

    override fun store(params: StoreAuthorizationRequestParams): AuthorizationRequest {
        logger.info { "Store authorization request, params: $params" }
        val record = AuthorizationRequestRecord(
            id = params.id,
            projectId = params.projectId,
            redirectUrl = params.redirectUrl,
            messageToSignOverride = params.messageToSignOverride,
            storeIndefinitely = params.storeIndefinitely,
            requestedWalletAddress = params.requestedWalletAddress,
            arbitraryData = params.arbitraryData,
            screenBeforeActionMessage = params.screenConfig.beforeActionMessage,
            screenAfterActionMessage = params.screenConfig.afterActionMessage,
            actualWalletAddress = null,
            signedMessage = null,
            createdAt = params.createdAt
        )
        dslContext.executeInsert(record)
        return record.toModel()
    }

    override fun delete(id: AuthorizationRequestId) {
        logger.info { "Deleting authorization request, id: $id" }
        dslContext.deleteFrom(AuthorizationRequestTable)
            .where(AuthorizationRequestTable.ID.eq(id))
            .execute()
    }

    override fun getById(id: AuthorizationRequestId): AuthorizationRequest? {
        logger.debug { "Get authorization request by id: $id" }
        return dslContext.selectFrom(AuthorizationRequestTable)
            .where(AuthorizationRequestTable.ID.eq(id))
            .fetchOne { it.toModel() }
    }

    override fun getAllByProjectId(projectId: ProjectId): List<AuthorizationRequest> {
        logger.debug { "Get authorization requests filtered by projectId: $projectId" }
        return dslContext.selectFrom(AuthorizationRequestTable)
            .where(AuthorizationRequestTable.PROJECT_ID.eq(projectId))
            .orderBy(AuthorizationRequestTable.CREATED_AT.asc())
            .fetch { it.toModel() }
    }

    override fun setSignedMessage(
        id: AuthorizationRequestId,
        walletAddress: WalletAddress,
        signedMessage: SignedMessage
    ): Boolean {
        logger.info {
            "Set walletAddress and signedMessage for authorization request, id: $id, walletAddress: $walletAddress," +
                " signedMessage: $signedMessage"
        }
        return dslContext.update(AuthorizationRequestTable)
            .set(AuthorizationRequestTable.ACTUAL_WALLET_ADDRESS, walletAddress)
            .set(AuthorizationRequestTable.SIGNED_MESSAGE, signedMessage)
            .where(
                DSL.and(
                    AuthorizationRequestTable.ID.eq(id),
                    AuthorizationRequestTable.ACTUAL_WALLET_ADDRESS.isNull(),
                    AuthorizationRequestTable.SIGNED_MESSAGE.isNull()
                )
            )
            .execute() > 0
    }

    private fun AuthorizationRequestRecord.toModel(): AuthorizationRequest =
        AuthorizationRequest(
            id = id,
            projectId = projectId,
            redirectUrl = redirectUrl,
            messageToSignOverride = messageToSignOverride,
            storeIndefinitely = storeIndefinitely,
            requestedWalletAddress = requestedWalletAddress,
            actualWalletAddress = actualWalletAddress,
            signedMessage = signedMessage,
            arbitraryData = arbitraryData,
            screenConfig = ScreenConfig(
                beforeActionMessage = screenBeforeActionMessage,
                afterActionMessage = screenAfterActionMessage
            ),
            createdAt = createdAt
        )
}
