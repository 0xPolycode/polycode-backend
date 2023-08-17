package polycode.features.asset.multisend.repository

import mu.KLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Repository
import polycode.features.asset.multisend.model.result.MultiPaymentTemplate
import polycode.features.asset.multisend.model.result.MultiPaymentTemplateItem
import polycode.features.asset.multisend.model.result.NoItems
import polycode.features.asset.multisend.model.result.WithItems
import polycode.generated.jooq.enums.UserIdentifierType
import polycode.generated.jooq.id.MultiPaymentTemplateId
import polycode.generated.jooq.id.MultiPaymentTemplateItemId
import polycode.generated.jooq.tables.MultiPaymentTemplateItemTable
import polycode.generated.jooq.tables.MultiPaymentTemplateTable
import polycode.generated.jooq.tables.UserIdentifierTable
import polycode.generated.jooq.tables.records.MultiPaymentTemplateItemRecord
import polycode.generated.jooq.tables.records.MultiPaymentTemplateRecord
import polycode.util.UtcDateTime
import polycode.util.WalletAddress

@Repository
@Suppress("TooManyFunctions")
class JooqMultiPaymentTemplateRepository(private val dslContext: DSLContext) : MultiPaymentTemplateRepository {

    companion object : KLogging()

    override fun store(multiPaymentTemplate: MultiPaymentTemplate<WithItems>): MultiPaymentTemplate<WithItems> {
        logger.info { "Store multi-payment template: $multiPaymentTemplate" }

        val record = MultiPaymentTemplateRecord(
            id = multiPaymentTemplate.id,
            templateName = multiPaymentTemplate.templateName,
            tokenAddress = multiPaymentTemplate.tokenAddress,
            chainId = multiPaymentTemplate.chainId,
            userId = multiPaymentTemplate.userId,
            createdAt = multiPaymentTemplate.createdAt,
            updatedAt = null
        )
        dslContext.executeInsert(record)

        val itemRecords = multiPaymentTemplate.items.value.map {
            MultiPaymentTemplateItemRecord(
                id = it.id,
                templateId = multiPaymentTemplate.id,
                walletAddress = it.walletAddress,
                itemName = it.itemName,
                assetAmount = it.assetAmount,
                createdAt = it.createdAt
            )
        }
        dslContext.batchInsert(itemRecords).execute()

        return record.toModel().withItems(itemRecords.map { it.toModel() })
    }

    override fun update(multiPaymentTemplate: MultiPaymentTemplate<*>): MultiPaymentTemplate<WithItems>? {
        logger.info { "Update multi-payment record, multiPaymentTemplate: $multiPaymentTemplate" }
        return dslContext.update(MultiPaymentTemplateTable)
            .set(MultiPaymentTemplateTable.TEMPLATE_NAME, multiPaymentTemplate.templateName)
            .set(MultiPaymentTemplateTable.TOKEN_ADDRESS, multiPaymentTemplate.tokenAddress)
            .set(MultiPaymentTemplateTable.CHAIN_ID, multiPaymentTemplate.chainId)
            .set(MultiPaymentTemplateTable.UPDATED_AT, multiPaymentTemplate.updatedAt)
            .where(
                DSL.and(
                    MultiPaymentTemplateTable.ID.eq(multiPaymentTemplate.id),
                    MultiPaymentTemplateTable.USER_ID.eq(multiPaymentTemplate.userId)
                )
            )
            .returning()
            .fetchOne { it.toModel() }
            ?.withItems(getItemsById(multiPaymentTemplate.id))
    }

    override fun delete(id: MultiPaymentTemplateId): Boolean {
        logger.info { "Delete multi-payment template, id: $id" }
        dslContext.deleteFrom(MultiPaymentTemplateItemTable)
            .where(MultiPaymentTemplateItemTable.TEMPLATE_ID.eq(id))
            .execute()
        return dslContext.deleteFrom(MultiPaymentTemplateTable)
            .where(MultiPaymentTemplateTable.ID.eq(id))
            .execute() > 0
    }

    override fun getById(id: MultiPaymentTemplateId): MultiPaymentTemplate<WithItems>? {
        logger.debug { "Get multi-payment template by id: $id" }
        return dslContext.selectFrom(MultiPaymentTemplateTable)
            .where(MultiPaymentTemplateTable.ID.eq(id))
            .fetchOne { it.toModel() }
            ?.withItems(getItemsById(id))
    }

    override fun getItemsById(id: MultiPaymentTemplateId): List<MultiPaymentTemplateItem> {
        logger.debug { "Get multi-payment template items by id: $id" }
        return dslContext.selectFrom(MultiPaymentTemplateItemTable)
            .where(MultiPaymentTemplateItemTable.TEMPLATE_ID.eq(id))
            .orderBy(MultiPaymentTemplateItemTable.CREATED_AT.asc())
            .fetch { it.toModel() }
    }

    override fun getAllByWalletAddress(walletAddress: WalletAddress): List<MultiPaymentTemplate<NoItems>> {
        logger.debug { "Get multi-payment templates by walletAddress: $walletAddress" }
        return dslContext.select(MultiPaymentTemplateTable.fields().toList())
            .from(
                MultiPaymentTemplateTable.join(UserIdentifierTable)
                    .on(MultiPaymentTemplateTable.USER_ID.eq(UserIdentifierTable.ID))
            )
            .where(
                DSL.and(
                    UserIdentifierTable.IDENTIFIER_TYPE.eq(UserIdentifierType.ETH_WALLET_ADDRESS),
                    UserIdentifierTable.USER_IDENTIFIER_.eq(walletAddress.rawValue)
                )
            )
            .orderBy(MultiPaymentTemplateTable.CREATED_AT.asc())
            .fetch { it.into(MultiPaymentTemplateTable).toModel() }
    }

    override fun addItem(item: MultiPaymentTemplateItem, updatedAt: UtcDateTime): MultiPaymentTemplate<WithItems>? {
        logger.info { "Store multi-payment template item, item: $item, updatedAt: $updatedAt" }

        try {
            dslContext.executeInsert(
                MultiPaymentTemplateItemRecord(
                    id = item.id,
                    templateId = item.templateId,
                    walletAddress = item.walletAddress,
                    itemName = item.itemName,
                    assetAmount = item.assetAmount,
                    createdAt = item.createdAt
                )
            )
        } catch (_: DataIntegrityViolationException) {
            return null
        }

        dslContext.update(MultiPaymentTemplateTable)
            .set(MultiPaymentTemplateTable.UPDATED_AT, updatedAt)
            .where(MultiPaymentTemplateTable.ID.eq(item.templateId))
            .execute()
        return getById(item.templateId)
    }

    override fun updateItem(item: MultiPaymentTemplateItem, updatedAt: UtcDateTime): MultiPaymentTemplate<WithItems>? {
        logger.info { "Update multi-payment record item, item: $item, updatedAt: $updatedAt" }
        dslContext.update(MultiPaymentTemplateItemTable)
            .set(MultiPaymentTemplateItemTable.WALLET_ADDRESS, item.walletAddress)
            .set(MultiPaymentTemplateItemTable.ITEM_NAME, item.itemName)
            .set(MultiPaymentTemplateItemTable.ASSET_AMOUNT, item.assetAmount)
            .where(MultiPaymentTemplateItemTable.ID.eq(item.id))
            .execute()
        dslContext.update(MultiPaymentTemplateTable)
            .set(MultiPaymentTemplateTable.UPDATED_AT, updatedAt)
            .where(MultiPaymentTemplateTable.ID.eq(item.templateId))
            .execute()
        return getById(item.templateId)
    }

    override fun deleteItem(
        templateId: MultiPaymentTemplateId,
        itemId: MultiPaymentTemplateItemId,
        updatedAt: UtcDateTime
    ): MultiPaymentTemplate<WithItems>? {
        logger.info { "Delete multi-payment template item, templateId: $templateId, itemId: $templateId" }
        dslContext.deleteFrom(MultiPaymentTemplateItemTable)
            .where(
                DSL.and(
                    MultiPaymentTemplateItemTable.ID.eq(itemId),
                    MultiPaymentTemplateItemTable.TEMPLATE_ID.eq(templateId)
                )
            )
            .execute()
        dslContext.update(MultiPaymentTemplateTable)
            .set(MultiPaymentTemplateTable.UPDATED_AT, updatedAt)
            .where(MultiPaymentTemplateTable.ID.eq(templateId))
            .execute()
        return getById(templateId)
    }

    private fun MultiPaymentTemplateRecord.toModel() =
        MultiPaymentTemplate(
            id = id,
            items = NoItems,
            templateName = templateName,
            tokenAddress = tokenAddress,
            chainId = chainId,
            userId = userId,
            createdAt = createdAt,
            updatedAt = updatedAt
        )

    private fun MultiPaymentTemplateItemRecord.toModel() =
        MultiPaymentTemplateItem(
            id = id,
            templateId = templateId,
            walletAddress = walletAddress,
            itemName = itemName,
            assetAmount = assetAmount,
            createdAt = createdAt
        )
}
