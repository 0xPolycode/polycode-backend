package polycode.features.asset.multisend.repository

import polycode.features.asset.multisend.model.result.MultiPaymentTemplate
import polycode.features.asset.multisend.model.result.MultiPaymentTemplateItem
import polycode.features.asset.multisend.model.result.NoItems
import polycode.features.asset.multisend.model.result.WithItems
import polycode.generated.jooq.id.MultiPaymentTemplateId
import polycode.generated.jooq.id.MultiPaymentTemplateItemId
import polycode.util.UtcDateTime
import polycode.util.WalletAddress

interface MultiPaymentTemplateRepository {
    fun store(multiPaymentTemplate: MultiPaymentTemplate<WithItems>): MultiPaymentTemplate<WithItems>
    fun update(multiPaymentTemplate: MultiPaymentTemplate<*>): MultiPaymentTemplate<WithItems>?
    fun delete(id: MultiPaymentTemplateId): Boolean
    fun getById(id: MultiPaymentTemplateId): MultiPaymentTemplate<WithItems>?
    fun getItemsById(id: MultiPaymentTemplateId): List<MultiPaymentTemplateItem>
    fun getAllByWalletAddress(walletAddress: WalletAddress): List<MultiPaymentTemplate<NoItems>>
    fun addItem(item: MultiPaymentTemplateItem, updatedAt: UtcDateTime): MultiPaymentTemplate<WithItems>?
    fun updateItem(item: MultiPaymentTemplateItem, updatedAt: UtcDateTime): MultiPaymentTemplate<WithItems>?
    fun deleteItem(
        templateId: MultiPaymentTemplateId,
        itemId: MultiPaymentTemplateItemId,
        updatedAt: UtcDateTime
    ): MultiPaymentTemplate<WithItems>?
}
