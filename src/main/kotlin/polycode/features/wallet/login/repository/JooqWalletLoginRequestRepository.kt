package polycode.features.wallet.login.repository

import mu.KLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import polycode.features.wallet.login.model.params.StoreWalletLoginRequestParams
import polycode.features.wallet.login.model.result.WalletLoginRequest
import polycode.generated.jooq.id.WalletLoginRequestId
import polycode.generated.jooq.tables.WalletLoginRequestTable
import polycode.generated.jooq.tables.records.WalletLoginRequestRecord
import polycode.util.SignedMessage

@Repository
class JooqWalletLoginRequestRepository(private val dslContext: DSLContext) : WalletLoginRequestRepository { // TODO test

    companion object : KLogging()

    override fun store(params: StoreWalletLoginRequestParams): WalletLoginRequest {
        logger.info { "Store wallet login request, params: $params" }
        val record = WalletLoginRequestRecord(
            id = params.id,
            walletAddress = params.walletAddress,
            messageToSign = params.messageToSign,
            signedMessage = null,
            createdAt = params.createdAt
        )
        dslContext.executeInsert(record)
        return record.toModel()
    }

    override fun getById(id: WalletLoginRequestId): WalletLoginRequest? {
        logger.debug { "Get wallet login request by id: $id" }
        return dslContext.selectFrom(WalletLoginRequestTable)
            .where(WalletLoginRequestTable.ID.eq(id))
            .fetchOne { it.toModel() }
    }

    override fun setSignedMessage(
        id: WalletLoginRequestId,
        signedMessage: SignedMessage
    ): Boolean {
        logger.info { "Set signedMessage for wallet login request, id: $id, signedMessage: $signedMessage" }
        return dslContext.update(WalletLoginRequestTable)
            .set(WalletLoginRequestTable.SIGNED_MESSAGE, signedMessage)
            .where(
                DSL.and(
                    WalletLoginRequestTable.ID.eq(id),
                    WalletLoginRequestTable.SIGNED_MESSAGE.isNull()
                )
            )
            .execute() > 0
    }

    private fun WalletLoginRequestRecord.toModel(): WalletLoginRequest =
        WalletLoginRequest(
            id = id,
            walletAddress = walletAddress,
            messageToSign = messageToSign,
            signedMessage = signedMessage,
            createdAt = createdAt
        )
}
