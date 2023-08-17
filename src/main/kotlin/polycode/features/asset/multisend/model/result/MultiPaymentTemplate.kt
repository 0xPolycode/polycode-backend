package polycode.features.asset.multisend.model.result

import polycode.generated.jooq.id.MultiPaymentTemplateId
import polycode.generated.jooq.id.MultiPaymentTemplateItemId
import polycode.generated.jooq.id.UserId
import polycode.util.Balance
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.UtcDateTime
import polycode.util.WalletAddress

sealed interface ItemsState

object NoItems : ItemsState

@JvmInline
value class WithItems(val value: List<MultiPaymentTemplateItem>) : ItemsState

data class MultiPaymentTemplate<T : ItemsState>(
    val id: MultiPaymentTemplateId,
    val items: T,
    val templateName: String,
    val tokenAddress: ContractAddress?,
    val chainId: ChainId,
    val userId: UserId,
    val createdAt: UtcDateTime,
    val updatedAt: UtcDateTime?
) {
    fun withItems(items: List<MultiPaymentTemplateItem>) =
        MultiPaymentTemplate(
            id = id,
            items = WithItems(items),
            templateName = templateName,
            tokenAddress = tokenAddress,
            chainId = chainId,
            userId = userId,
            createdAt = createdAt,
            updatedAt = updatedAt
        )

    fun withoutItems() =
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
}

data class MultiPaymentTemplateItem(
    val id: MultiPaymentTemplateItemId,
    val templateId: MultiPaymentTemplateId,
    val walletAddress: WalletAddress,
    val itemName: String?,
    val assetAmount: Balance,
    val createdAt: UtcDateTime
)
