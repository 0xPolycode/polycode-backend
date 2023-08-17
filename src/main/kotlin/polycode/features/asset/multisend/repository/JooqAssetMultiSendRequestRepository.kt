package polycode.features.asset.multisend.repository

import mu.KLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.impl.DSL.coalesce
import org.springframework.stereotype.Repository
import polycode.features.asset.multisend.model.params.StoreAssetMultiSendRequestParams
import polycode.features.asset.multisend.model.result.AssetMultiSendRequest
import polycode.generated.jooq.id.AssetMultiSendRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.tables.AssetMultiSendRequestTable
import polycode.generated.jooq.tables.records.AssetMultiSendRequestRecord
import polycode.model.ScreenConfig
import polycode.util.Balance
import polycode.util.TransactionHash
import polycode.util.WalletAddress

@Repository
class JooqAssetMultiSendRequestRepository(private val dslContext: DSLContext) : AssetMultiSendRequestRepository {

    companion object : KLogging()

    override fun store(params: StoreAssetMultiSendRequestParams): AssetMultiSendRequest {
        logger.info { "Store asset multi-send request, params: $params" }
        @Suppress("UNCHECKED_CAST") // we allow nullable values for itemNames
        val record = AssetMultiSendRequestRecord(
            id = params.id,
            chainId = params.chainId,
            redirectUrl = params.redirectUrl,
            tokenAddress = params.tokenAddress,
            disperseContractAddress = params.disperseContractAddress,
            assetAmounts = params.assetAmounts.map { it.rawValue.toBigDecimal() }.toTypedArray(),
            assetRecipientAddresses = params.assetRecipientAddresses.map { it.rawValue }.toTypedArray(),
            itemNames = params.itemNames.toTypedArray() as Array<String>,
            assetSenderAddress = params.assetSenderAddress,
            arbitraryData = params.arbitraryData,
            approveTxHash = null,
            disperseTxHash = null,
            approveScreenBeforeActionMessage = params.approveScreenConfig.beforeActionMessage,
            approveScreenAfterActionMessage = params.approveScreenConfig.afterActionMessage,
            disperseScreenBeforeActionMessage = params.disperseScreenConfig.beforeActionMessage,
            disperseScreenAfterActionMessage = params.disperseScreenConfig.afterActionMessage,
            projectId = params.projectId,
            createdAt = params.createdAt
        )
        dslContext.executeInsert(record)
        return record.toModel()
    }

    override fun getById(id: AssetMultiSendRequestId): AssetMultiSendRequest? {
        logger.debug { "Get asset multi-send request by id: $id" }
        return dslContext.selectFrom(AssetMultiSendRequestTable)
            .where(AssetMultiSendRequestTable.ID.eq(id))
            .fetchOne { it.toModel() }
    }

    override fun getAllByProjectId(projectId: ProjectId): List<AssetMultiSendRequest> {
        logger.debug { "Get asset multi-send requests filtered by projectId: $projectId" }
        return dslContext.selectFrom(AssetMultiSendRequestTable)
            .where(AssetMultiSendRequestTable.PROJECT_ID.eq(projectId))
            .orderBy(AssetMultiSendRequestTable.CREATED_AT.asc())
            .fetch { it.toModel() }
    }

    override fun getBySender(sender: WalletAddress): List<AssetMultiSendRequest> {
        logger.debug { "Get asset multi-send requests filtered by sender address: $sender" }
        return dslContext.selectFrom(AssetMultiSendRequestTable)
            .where(AssetMultiSendRequestTable.ASSET_SENDER_ADDRESS.eq(sender))
            .orderBy(AssetMultiSendRequestTable.CREATED_AT.asc())
            .fetch { it.toModel() }
    }

    override fun setApproveTxInfo(
        id: AssetMultiSendRequestId,
        txHash: TransactionHash,
        caller: WalletAddress
    ): Boolean {
        logger.info { "Set approve txInfo for asset multi-send request, id: $id, txHash: $txHash, caller: $caller" }
        return dslContext.update(AssetMultiSendRequestTable)
            .set(AssetMultiSendRequestTable.APPROVE_TX_HASH, txHash)
            .set(
                AssetMultiSendRequestTable.ASSET_SENDER_ADDRESS,
                coalesce(AssetMultiSendRequestTable.ASSET_SENDER_ADDRESS, caller)
            )
            .where(
                DSL.and(
                    AssetMultiSendRequestTable.ID.eq(id),
                    AssetMultiSendRequestTable.APPROVE_TX_HASH.isNull(),
                    AssetMultiSendRequestTable.TOKEN_ADDRESS.isNotNull()
                )
            )
            .execute() > 0
    }

    override fun setDisperseTxInfo(
        id: AssetMultiSendRequestId,
        txHash: TransactionHash,
        caller: WalletAddress
    ): Boolean {
        logger.info { "Set disperse txInfo for asset multi-send request, id: $id, txHash: $txHash, caller: $caller" }
        return dslContext.update(AssetMultiSendRequestTable)
            .set(AssetMultiSendRequestTable.DISPERSE_TX_HASH, txHash)
            .set(
                AssetMultiSendRequestTable.ASSET_SENDER_ADDRESS,
                coalesce(AssetMultiSendRequestTable.ASSET_SENDER_ADDRESS, caller)
            )
            .where(
                DSL.and(
                    AssetMultiSendRequestTable.ID.eq(id),
                    AssetMultiSendRequestTable.DISPERSE_TX_HASH.isNull(),
                    DSL.or(
                        AssetMultiSendRequestTable.APPROVE_TX_HASH.isNotNull(),
                        AssetMultiSendRequestTable.TOKEN_ADDRESS.isNull()
                    )
                )
            )
            .execute() > 0
    }

    private fun AssetMultiSendRequestRecord.toModel(): AssetMultiSendRequest =
        AssetMultiSendRequest(
            id = id,
            projectId = projectId,
            chainId = chainId,
            redirectUrl = redirectUrl,
            tokenAddress = tokenAddress,
            disperseContractAddress = disperseContractAddress,
            assetAmounts = assetAmounts.map { Balance(it.toBigInteger()) }.toList(),
            assetRecipientAddresses = assetRecipientAddresses.map { WalletAddress(it) }.toList(),
            itemNames = itemNames.toList(),
            assetSenderAddress = assetSenderAddress,
            approveTxHash = approveTxHash,
            disperseTxHash = disperseTxHash,
            arbitraryData = arbitraryData,
            approveScreenConfig = ScreenConfig(
                beforeActionMessage = approveScreenBeforeActionMessage,
                afterActionMessage = approveScreenAfterActionMessage
            ),
            disperseScreenConfig = ScreenConfig(
                beforeActionMessage = disperseScreenBeforeActionMessage,
                afterActionMessage = disperseScreenAfterActionMessage
            ),
            createdAt = createdAt
        )
}
