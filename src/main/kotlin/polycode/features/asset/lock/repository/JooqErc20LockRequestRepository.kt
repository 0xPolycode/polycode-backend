package polycode.features.asset.lock.repository

import mu.KLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.impl.DSL.coalesce
import org.springframework.stereotype.Repository
import polycode.features.asset.lock.model.params.StoreErc20LockRequestParams
import polycode.features.asset.lock.model.result.Erc20LockRequest
import polycode.generated.jooq.id.Erc20LockRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.tables.Erc20LockRequestTable
import polycode.generated.jooq.tables.records.Erc20LockRequestRecord
import polycode.model.ScreenConfig
import polycode.util.TransactionHash
import polycode.util.WalletAddress

@Repository
class JooqErc20LockRequestRepository(private val dslContext: DSLContext) : Erc20LockRequestRepository {

    companion object : KLogging()

    override fun store(params: StoreErc20LockRequestParams): Erc20LockRequest {
        logger.info { "Store ERC20 lock request, params: $params" }
        val record = Erc20LockRequestRecord(
            id = params.id,
            projectId = params.projectId,
            chainId = params.chainId,
            redirectUrl = params.redirectUrl,
            tokenAddress = params.tokenAddress,
            tokenAmount = params.tokenAmount,
            lockDurationSeconds = params.lockDuration,
            lockContractAddress = params.lockContractAddress,
            tokenSenderAddress = params.tokenSenderAddress,
            arbitraryData = params.arbitraryData,
            screenBeforeActionMessage = params.screenConfig.beforeActionMessage,
            screenAfterActionMessage = params.screenConfig.afterActionMessage,
            txHash = null,
            createdAt = params.createdAt
        )
        dslContext.executeInsert(record)
        return record.toModel()
    }

    override fun getById(id: Erc20LockRequestId): Erc20LockRequest? {
        logger.debug { "Get ERC20 lock request by id: $id" }
        return dslContext.selectFrom(Erc20LockRequestTable)
            .where(Erc20LockRequestTable.ID.eq(id))
            .fetchOne { it.toModel() }
    }

    override fun getAllByProjectId(projectId: ProjectId): List<Erc20LockRequest> {
        logger.debug { "Get ERC20 lock requests filtered by projectId: $projectId" }
        return dslContext.selectFrom(Erc20LockRequestTable)
            .where(Erc20LockRequestTable.PROJECT_ID.eq(projectId))
            .orderBy(Erc20LockRequestTable.CREATED_AT.asc())
            .fetch { it.toModel() }
    }

    override fun setTxInfo(id: Erc20LockRequestId, txHash: TransactionHash, caller: WalletAddress): Boolean {
        logger.info { "Set txInfo for ERC20 lock request, id: $id, txHash: $txHash, caller: $caller" }
        return dslContext.update(Erc20LockRequestTable)
            .set(Erc20LockRequestTable.TX_HASH, txHash)
            .set(
                Erc20LockRequestTable.TOKEN_SENDER_ADDRESS,
                coalesce(Erc20LockRequestTable.TOKEN_SENDER_ADDRESS, caller)
            )
            .where(
                DSL.and(
                    Erc20LockRequestTable.ID.eq(id),
                    Erc20LockRequestTable.TX_HASH.isNull()
                )
            )
            .execute() > 0
    }

    private fun Erc20LockRequestRecord.toModel(): Erc20LockRequest =
        Erc20LockRequest(
            id = id,
            projectId = projectId,
            chainId = chainId,
            redirectUrl = redirectUrl,
            tokenAddress = tokenAddress,
            tokenAmount = tokenAmount,
            lockDuration = lockDurationSeconds,
            lockContractAddress = lockContractAddress,
            tokenSenderAddress = tokenSenderAddress,
            txHash = txHash,
            arbitraryData = arbitraryData,
            screenConfig = ScreenConfig(
                beforeActionMessage = screenBeforeActionMessage,
                afterActionMessage = screenAfterActionMessage
            ),
            createdAt = createdAt
        )
}
