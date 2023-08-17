package polycode.features.asset.balance.repository

import mu.KLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import polycode.features.asset.balance.model.params.StoreAssetBalanceRequestParams
import polycode.features.asset.balance.model.result.AssetBalanceRequest
import polycode.generated.jooq.id.AssetBalanceRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.tables.AssetBalanceRequestTable
import polycode.generated.jooq.tables.records.AssetBalanceRequestRecord
import polycode.model.ScreenConfig
import polycode.util.SignedMessage
import polycode.util.WalletAddress

@Repository
class JooqAssetBalanceRequestRepository(private val dslContext: DSLContext) : AssetBalanceRequestRepository {

    companion object : KLogging()

    override fun store(params: StoreAssetBalanceRequestParams): AssetBalanceRequest {
        logger.info { "Store asset balance request, params: $params" }
        val record = AssetBalanceRequestRecord(
            id = params.id,
            projectId = params.projectId,
            chainId = params.chainId,
            redirectUrl = params.redirectUrl,
            tokenAddress = params.tokenAddress,
            blockNumber = params.blockNumber,
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

    override fun getById(id: AssetBalanceRequestId): AssetBalanceRequest? {
        logger.debug { "Get asset balance request by id: $id" }
        return dslContext.selectFrom(AssetBalanceRequestTable)
            .where(AssetBalanceRequestTable.ID.eq(id))
            .fetchOne { it.toModel() }
    }

    override fun getAllByProjectId(projectId: ProjectId): List<AssetBalanceRequest> {
        logger.debug { "Get asset balance requests filtered by projectId: $projectId" }
        return dslContext.selectFrom(AssetBalanceRequestTable)
            .where(AssetBalanceRequestTable.PROJECT_ID.eq(projectId))
            .orderBy(AssetBalanceRequestTable.CREATED_AT.asc())
            .fetch { it.toModel() }
    }

    override fun setSignedMessage(
        id: AssetBalanceRequestId,
        walletAddress: WalletAddress,
        signedMessage: SignedMessage
    ): Boolean {
        logger.info {
            "Set walletAddress and signedMessage for asset balance request, id: $id, walletAddress: $walletAddress," +
                " signedMessage: $signedMessage"
        }
        return dslContext.update(AssetBalanceRequestTable)
            .set(AssetBalanceRequestTable.ACTUAL_WALLET_ADDRESS, walletAddress)
            .set(AssetBalanceRequestTable.SIGNED_MESSAGE, signedMessage)
            .where(
                DSL.and(
                    AssetBalanceRequestTable.ID.eq(id),
                    AssetBalanceRequestTable.ACTUAL_WALLET_ADDRESS.isNull(),
                    AssetBalanceRequestTable.SIGNED_MESSAGE.isNull()
                )
            )
            .execute() > 0
    }

    private fun AssetBalanceRequestRecord.toModel(): AssetBalanceRequest =
        AssetBalanceRequest(
            id = id,
            projectId = projectId,
            chainId = chainId,
            redirectUrl = redirectUrl,
            tokenAddress = tokenAddress,
            blockNumber = blockNumber,
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
