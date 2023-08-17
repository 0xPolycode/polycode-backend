package polycode.features.asset.multisend.service

import polycode.features.api.access.model.result.UserIdentifier
import polycode.features.asset.multisend.model.request.CreateMultiPaymentTemplateRequest
import polycode.features.asset.multisend.model.request.MultiPaymentTemplateItemRequest
import polycode.features.asset.multisend.model.request.UpdateMultiPaymentTemplateRequest
import polycode.features.asset.multisend.model.result.MultiPaymentTemplate
import polycode.features.asset.multisend.model.result.NoItems
import polycode.features.asset.multisend.model.result.WithItems
import polycode.generated.jooq.id.MultiPaymentTemplateId
import polycode.generated.jooq.id.MultiPaymentTemplateItemId
import polycode.util.WalletAddress

interface MultiPaymentTemplateService {
    fun createMultiPaymentTemplate(
        request: CreateMultiPaymentTemplateRequest,
        userIdentifier: UserIdentifier
    ): MultiPaymentTemplate<WithItems>

    fun updateMultiPaymentTemplate(
        templateId: MultiPaymentTemplateId,
        request: UpdateMultiPaymentTemplateRequest,
        userIdentifier: UserIdentifier
    ): MultiPaymentTemplate<WithItems>

    fun deleteMultiPaymentTemplateById(templateId: MultiPaymentTemplateId, userIdentifier: UserIdentifier)
    fun getMultiPaymentTemplateById(templateId: MultiPaymentTemplateId): MultiPaymentTemplate<WithItems>
    fun getAllMultiPaymentTemplatesByWalletAddress(walletAddress: WalletAddress): List<MultiPaymentTemplate<NoItems>>
    fun addItemToMultiPaymentTemplate(
        templateId: MultiPaymentTemplateId,
        request: MultiPaymentTemplateItemRequest,
        userIdentifier: UserIdentifier
    ): MultiPaymentTemplate<WithItems>

    fun updateMultiPaymentTemplateItem(
        templateId: MultiPaymentTemplateId,
        itemId: MultiPaymentTemplateItemId,
        request: MultiPaymentTemplateItemRequest,
        userIdentifier: UserIdentifier
    ): MultiPaymentTemplate<WithItems>

    fun deleteMultiPaymentTemplateItem(
        templateId: MultiPaymentTemplateId,
        itemId: MultiPaymentTemplateItemId,
        userIdentifier: UserIdentifier
    ): MultiPaymentTemplate<WithItems>
}
