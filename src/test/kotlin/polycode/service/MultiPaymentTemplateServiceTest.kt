package polycode.service

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import polycode.TestBase
import polycode.TestData
import polycode.exception.ResourceNotFoundException
import polycode.features.api.access.model.result.UserWalletAddressIdentifier
import polycode.features.asset.multisend.model.request.CreateMultiPaymentTemplateRequest
import polycode.features.asset.multisend.model.request.MultiPaymentTemplateItemRequest
import polycode.features.asset.multisend.model.request.UpdateMultiPaymentTemplateRequest
import polycode.features.asset.multisend.model.result.MultiPaymentTemplate
import polycode.features.asset.multisend.model.result.MultiPaymentTemplateItem
import polycode.features.asset.multisend.model.result.WithItems
import polycode.features.asset.multisend.repository.MultiPaymentTemplateRepository
import polycode.features.asset.multisend.service.MultiPaymentTemplateServiceImpl
import polycode.generated.jooq.id.MultiPaymentTemplateId
import polycode.generated.jooq.id.MultiPaymentTemplateItemId
import polycode.generated.jooq.id.UserId
import polycode.util.AssetType
import polycode.util.Balance
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.WalletAddress
import java.math.BigInteger
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

class MultiPaymentTemplateServiceTest : TestBase() {

    companion object {
        private val USER_IDENTIFIER = UserWalletAddressIdentifier(
            id = UserId(UUID.randomUUID()),
            walletAddress = WalletAddress("cafebabe")
        )
        private val TEMPLATE_ID = MultiPaymentTemplateId(UUID.randomUUID())
        private val ITEM = MultiPaymentTemplateItem(
            id = MultiPaymentTemplateItemId(UUID.randomUUID()),
            templateId = TEMPLATE_ID,
            walletAddress = WalletAddress("a"),
            itemName = "itemName",
            assetAmount = Balance(BigInteger.TEN),
            createdAt = TestData.TIMESTAMP
        )
        private val TEMPLATE = MultiPaymentTemplate(
            id = TEMPLATE_ID,
            items = WithItems(listOf(ITEM)),
            templateName = "templateName",
            tokenAddress = ContractAddress("b"),
            chainId = ChainId(1337L),
            userId = USER_IDENTIFIER.id,
            createdAt = TestData.TIMESTAMP,
            updatedAt = null
        )
        private val ITEM_REQUEST = MultiPaymentTemplateItemRequest(
            walletAddress = ITEM.walletAddress.rawValue,
            itemName = ITEM.itemName,
            amount = ITEM.assetAmount.rawValue
        )
        private val CREATE_REQUEST = CreateMultiPaymentTemplateRequest(
            templateName = TEMPLATE.templateName,
            assetType = AssetType.TOKEN,
            tokenAddress = TEMPLATE.tokenAddress?.rawValue,
            chainId = TEMPLATE.chainId.value,
            items = listOf(ITEM_REQUEST)
        )
        private val UPDATE_REQUEST = UpdateMultiPaymentTemplateRequest(
            templateName = "newName",
            assetType = AssetType.NATIVE,
            tokenAddress = null,
            chainId = 123L
        )
    }

    @Test
    fun mustSuccessfullyCreateMultiPaymentTemplate() {
        val uuidProvider = mock<UuidProvider>()

        suppose("some UUID will be returned") {
            call(uuidProvider.getUuid(MultiPaymentTemplateId))
                .willReturn(TEMPLATE_ID)
            call(uuidProvider.getUuid(MultiPaymentTemplateItemId))
                .willReturn(ITEM.id)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()

        suppose("some timestamp will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(ITEM.createdAt, TEMPLATE.createdAt)
        }

        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("multi-payment template will be stored into the database") {
            call(multiPaymentTemplateRepository.store(TEMPLATE))
                .willReturn(TEMPLATE)
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = uuidProvider,
            utcDateTimeProvider = utcDateTimeProvider
        )

        verify("multi-payment template is stored into the database") {
            expectThat(service.createMultiPaymentTemplate(CREATE_REQUEST, USER_IDENTIFIER))
                .isEqualTo(TEMPLATE)

            expectInteractions(multiPaymentTemplateRepository) {
                once.store(TEMPLATE)
            }
        }
    }

    @Test
    fun mustCorrectlyUpdateMultiPaymentTemplate() {
        val utcDateTimeProvider = mock<UtcDateTimeProvider>()
        val updatedAt = TestData.TIMESTAMP + 10.seconds

        suppose("some timestamp will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(updatedAt)
        }

        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("multi-payment template is fetched by id") {
            call(multiPaymentTemplateRepository.getById(TEMPLATE_ID))
                .willReturn(TEMPLATE)
        }

        val updatedTemplate = TEMPLATE.copy(
            templateName = UPDATE_REQUEST.templateName,
            tokenAddress = UPDATE_REQUEST.tokenAddress?.let { ContractAddress(it) },
            chainId = ChainId(UPDATE_REQUEST.chainId),
            updatedAt = updatedAt
        )

        suppose("multi-payment template will be updated in the database") {
            call(multiPaymentTemplateRepository.update(updatedTemplate))
                .willReturn(updatedTemplate)
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = utcDateTimeProvider
        )

        verify("multi-payment template is updated in the database") {
            expectThat(service.updateMultiPaymentTemplate(TEMPLATE_ID, UPDATE_REQUEST, USER_IDENTIFIER))
                .isEqualTo(updatedTemplate)

            expectInteractions(multiPaymentTemplateRepository) {
                once.getById(TEMPLATE_ID)
                once.update(updatedTemplate)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenUpdatingNonOwnedMultiPaymentTemplate() {
        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("non-owned multi-payment template is fetched by id") {
            call(multiPaymentTemplateRepository.getById(TEMPLATE_ID))
                .willReturn(TEMPLATE.copy(userId = UserId(UUID.randomUUID())))
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.updateMultiPaymentTemplate(TEMPLATE_ID, UPDATE_REQUEST, USER_IDENTIFIER)
            }

            expectInteractions(multiPaymentTemplateRepository) {
                once.getById(TEMPLATE_ID)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenUpdatingNonExistentMultiPaymentTemplate() {
        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("multi-payment template is fetched by id") {
            call(multiPaymentTemplateRepository.getById(TEMPLATE_ID))
                .willReturn(TEMPLATE)
        }

        val updatedTemplate = TEMPLATE.copy(
            templateName = UPDATE_REQUEST.templateName,
            tokenAddress = UPDATE_REQUEST.tokenAddress?.let { ContractAddress(it) },
            chainId = ChainId(UPDATE_REQUEST.chainId)
        )

        suppose("null will be returned when updating multi-payment template") {
            call(multiPaymentTemplateRepository.update(updatedTemplate))
                .willReturn(null)
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.updateMultiPaymentTemplate(TEMPLATE_ID, UPDATE_REQUEST, USER_IDENTIFIER)
            }

            expectInteractions(multiPaymentTemplateRepository) {
                once.getById(TEMPLATE_ID)
                once.update(updatedTemplate)
            }
        }
    }

    @Test
    fun mustSuccessfullyDeleteMultiPaymentTemplate() {
        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("multi-payment template is fetched by id") {
            call(multiPaymentTemplateRepository.getById(TEMPLATE_ID))
                .willReturn(TEMPLATE)
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        suppose("multi-payment template is deleted from the database") {
            service.deleteMultiPaymentTemplateById(TEMPLATE_ID, USER_IDENTIFIER)
        }

        verify("multi-payment template was deleted from the database") {
            expectInteractions(multiPaymentTemplateRepository) {
                once.getById(TEMPLATE_ID)
                once.delete(TEMPLATE_ID)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenDeletingNonOwnedMultiPaymentTemplate() {
        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("non-owned multi-payment template is fetched by id") {
            call(multiPaymentTemplateRepository.getById(TEMPLATE_ID))
                .willReturn(TEMPLATE.copy(userId = UserId(UUID.randomUUID())))
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.deleteMultiPaymentTemplateById(TEMPLATE_ID, USER_IDENTIFIER)
            }

            expectInteractions(multiPaymentTemplateRepository) {
                once.getById(TEMPLATE_ID)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenDeletingNonExistentMultiPaymentTemplate() {
        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("null is returned when fetching multi-payment template by id") {
            call(multiPaymentTemplateRepository.getById(TEMPLATE_ID))
                .willReturn(null)
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.deleteMultiPaymentTemplateById(TEMPLATE_ID, USER_IDENTIFIER)
            }

            expectInteractions(multiPaymentTemplateRepository) {
                once.getById(TEMPLATE_ID)
            }
        }
    }

    @Test
    fun mustSuccessfullyGetMultiPaymentTemplateById() {
        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("multi-payment template is fetched by id") {
            call(multiPaymentTemplateRepository.getById(TEMPLATE_ID))
                .willReturn(TEMPLATE)
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("multi-payment template is updated in the database") {
            expectThat(service.getMultiPaymentTemplateById(TEMPLATE_ID))
                .isEqualTo(TEMPLATE)

            expectInteractions(multiPaymentTemplateRepository) {
                once.getById(TEMPLATE_ID)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenFetchingNonExistentMultiPaymentTemplate() {
        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("null is returned when fetching multi-payment template by id") {
            call(multiPaymentTemplateRepository.getById(TEMPLATE_ID))
                .willReturn(null)
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.getMultiPaymentTemplateById(TEMPLATE_ID)
            }

            expectInteractions(multiPaymentTemplateRepository) {
                once.getById(TEMPLATE_ID)
            }
        }
    }

    @Test
    fun mustSuccessfullyGetMultiPaymentTemplatesByWalletAddress() {
        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("multi-payment templates are fetched by wallet address") {
            call(multiPaymentTemplateRepository.getAllByWalletAddress(USER_IDENTIFIER.walletAddress))
                .willReturn(listOf(TEMPLATE.withoutItems()))
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("multi-payment template is updated in the database") {
            expectThat(service.getAllMultiPaymentTemplatesByWalletAddress(USER_IDENTIFIER.walletAddress))
                .isEqualTo(listOf(TEMPLATE.withoutItems()))

            expectInteractions(multiPaymentTemplateRepository) {
                once.getAllByWalletAddress(USER_IDENTIFIER.walletAddress)
            }
        }
    }

    @Test
    fun mustSuccessfullyAddItemToMultiPaymentTemplate() {
        val uuidProvider = mock<UuidProvider>()
        val newItemId = MultiPaymentTemplateItemId(UUID.randomUUID())

        suppose("some UUID will be returned") {
            call(uuidProvider.getUuid(MultiPaymentTemplateItemId))
                .willReturn(newItemId)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()
        val newItemTimestamp = TestData.TIMESTAMP + 1.seconds

        suppose("some timestamp will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(newItemTimestamp)
        }

        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("multi-payment template is fetched by id") {
            call(multiPaymentTemplateRepository.getById(TEMPLATE_ID))
                .willReturn(TEMPLATE)
        }

        val newItem = MultiPaymentTemplateItem(
            id = newItemId,
            templateId = TEMPLATE_ID,
            walletAddress = WalletAddress(ITEM_REQUEST.walletAddress),
            itemName = ITEM_REQUEST.itemName,
            assetAmount = Balance(ITEM_REQUEST.amount),
            createdAt = newItemTimestamp
        )
        val updatedTemplate = TEMPLATE.copy(
            items = WithItems(TEMPLATE.items.value + newItem),
            updatedAt = newItemTimestamp
        )

        suppose("item will be added to multi-payment template in the database") {
            call(multiPaymentTemplateRepository.addItem(newItem, newItemTimestamp))
                .willReturn(updatedTemplate)
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = uuidProvider,
            utcDateTimeProvider = utcDateTimeProvider
        )

        verify("item is added to multi-payment template the database") {
            expectThat(service.addItemToMultiPaymentTemplate(TEMPLATE_ID, ITEM_REQUEST, USER_IDENTIFIER))
                .isEqualTo(updatedTemplate)

            expectInteractions(multiPaymentTemplateRepository) {
                once.getById(TEMPLATE_ID)
                once.addItem(newItem, newItemTimestamp)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenAddingMultiTemplateItemToNonOwnedTemplate() {
        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("non-owned multi-payment template is fetched by id") {
            call(multiPaymentTemplateRepository.getById(TEMPLATE_ID))
                .willReturn(TEMPLATE.copy(userId = UserId(UUID.randomUUID())))
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.addItemToMultiPaymentTemplate(TEMPLATE_ID, ITEM_REQUEST, USER_IDENTIFIER)
            }

            expectInteractions(multiPaymentTemplateRepository) {
                once.getById(TEMPLATE_ID)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenAddingMultiTemplateItemToNonExistentTemplate() {
        val uuidProvider = mock<UuidProvider>()
        val newItemId = MultiPaymentTemplateItemId(UUID.randomUUID())

        suppose("some UUID will be returned") {
            call(uuidProvider.getUuid(MultiPaymentTemplateItemId))
                .willReturn(newItemId)
        }

        val utcDateTimeProvider = mock<UtcDateTimeProvider>()
        val newItemTimestamp = TestData.TIMESTAMP + 1.seconds

        suppose("some timestamp will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(newItemTimestamp)
        }

        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("multi-payment template is fetched by id") {
            call(multiPaymentTemplateRepository.getById(TEMPLATE_ID))
                .willReturn(TEMPLATE)
        }

        val newItem = MultiPaymentTemplateItem(
            id = newItemId,
            templateId = TEMPLATE_ID,
            walletAddress = WalletAddress(ITEM_REQUEST.walletAddress),
            itemName = ITEM_REQUEST.itemName,
            assetAmount = Balance(ITEM_REQUEST.amount),
            createdAt = newItemTimestamp
        )

        suppose("null will be returned when adding item to multi-payment template in the database") {
            call(multiPaymentTemplateRepository.addItem(newItem, newItemTimestamp))
                .willReturn(null)
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = uuidProvider,
            utcDateTimeProvider = utcDateTimeProvider
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.addItemToMultiPaymentTemplate(TEMPLATE_ID, ITEM_REQUEST, USER_IDENTIFIER)
            }

            expectInteractions(multiPaymentTemplateRepository) {
                once.getById(TEMPLATE_ID)
                once.addItem(newItem, newItemTimestamp)
            }
        }
    }

    @Test
    fun mustSuccessfullyUpdateMultiPaymentTemplateItem() {
        val utcDateTimeProvider = mock<UtcDateTimeProvider>()
        val updateTimestamp = TestData.TIMESTAMP + 1.seconds

        suppose("some timestamp will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(updateTimestamp)
        }

        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("multi-payment template is fetched by id") {
            call(multiPaymentTemplateRepository.getById(TEMPLATE_ID))
                .willReturn(TEMPLATE)
        }

        val updatedItem = MultiPaymentTemplateItem(
            id = ITEM.id,
            templateId = TEMPLATE_ID,
            walletAddress = WalletAddress("abc123"),
            itemName = "updatedItemName",
            assetAmount = Balance(BigInteger.valueOf(123L)),
            createdAt = updateTimestamp
        )
        val updatedTemplate = TEMPLATE.copy(
            items = WithItems(listOf(updatedItem)),
            updatedAt = updateTimestamp
        )

        suppose("item will be updated for multi-payment template in the database") {
            call(multiPaymentTemplateRepository.updateItem(updatedItem, updateTimestamp))
                .willReturn(updatedTemplate)
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = utcDateTimeProvider
        )

        val updateItemRequest = MultiPaymentTemplateItemRequest(
            walletAddress = updatedItem.walletAddress.rawValue,
            itemName = updatedItem.itemName,
            amount = updatedItem.assetAmount.rawValue
        )

        verify("item is updated for multi-payment template the database") {
            expectThat(service.updateMultiPaymentTemplateItem(TEMPLATE_ID, ITEM.id, updateItemRequest, USER_IDENTIFIER))
                .isEqualTo(updatedTemplate)

            expectInteractions(multiPaymentTemplateRepository) {
                once.getById(TEMPLATE_ID)
                once.updateItem(updatedItem, updateTimestamp)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenUpdatingMultiTemplateItemForNonOwnedTemplate() {
        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("non-owned multi-payment template is fetched by id") {
            call(multiPaymentTemplateRepository.getById(TEMPLATE_ID))
                .willReturn(TEMPLATE.copy(userId = UserId(UUID.randomUUID())))
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.updateMultiPaymentTemplateItem(TEMPLATE_ID, ITEM.id, ITEM_REQUEST, USER_IDENTIFIER)
            }

            expectInteractions(multiPaymentTemplateRepository) {
                once.getById(TEMPLATE_ID)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenUpdatingMultiTemplateItemForNonExistentTemplate() {
        val utcDateTimeProvider = mock<UtcDateTimeProvider>()
        val updateTimestamp = TestData.TIMESTAMP + 1.seconds

        suppose("some timestamp will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(updateTimestamp)
        }

        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("multi-payment template is fetched by id") {
            call(multiPaymentTemplateRepository.getById(TEMPLATE_ID))
                .willReturn(TEMPLATE)
        }

        val updatedItem = MultiPaymentTemplateItem(
            id = ITEM.id,
            templateId = TEMPLATE_ID,
            walletAddress = WalletAddress("abc123"),
            itemName = "updatedItemName",
            assetAmount = Balance(BigInteger.valueOf(123L)),
            createdAt = updateTimestamp
        )

        suppose("null will be returned when updating multi-payment template item in the database") {
            call(multiPaymentTemplateRepository.updateItem(updatedItem, updateTimestamp))
                .willReturn(null)
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = utcDateTimeProvider
        )

        val updateItemRequest = MultiPaymentTemplateItemRequest(
            walletAddress = updatedItem.walletAddress.rawValue,
            itemName = updatedItem.itemName,
            amount = updatedItem.assetAmount.rawValue
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.updateMultiPaymentTemplateItem(TEMPLATE_ID, ITEM.id, updateItemRequest, USER_IDENTIFIER)
            }

            expectInteractions(multiPaymentTemplateRepository) {
                once.getById(TEMPLATE_ID)
                once.updateItem(updatedItem, updateTimestamp)
            }
        }
    }

    @Test
    fun mustSuccessfullyDeleteMultiPaymentTemplateItem() {
        val utcDateTimeProvider = mock<UtcDateTimeProvider>()
        val updateTimestamp = TestData.TIMESTAMP + 1.seconds

        suppose("some timestamp will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(updateTimestamp)
        }

        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("multi-payment template is fetched by id") {
            call(multiPaymentTemplateRepository.getById(TEMPLATE_ID))
                .willReturn(TEMPLATE)
        }

        suppose("multi-payment template item will be deleted from the database") {
            call(multiPaymentTemplateRepository.deleteItem(TEMPLATE_ID, ITEM.id, updateTimestamp))
                .willReturn(TEMPLATE.copy(items = WithItems(emptyList())))
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = utcDateTimeProvider
        )

        verify("multi-payment template item was deleted from the database") {
            expectThat(service.deleteMultiPaymentTemplateItem(TEMPLATE_ID, ITEM.id, USER_IDENTIFIER))
                .isEqualTo(TEMPLATE.copy(items = WithItems(emptyList())))

            expectInteractions(multiPaymentTemplateRepository) {
                once.getById(TEMPLATE_ID)
                once.deleteItem(TEMPLATE_ID, ITEM.id, updateTimestamp)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenDeletingMultiTemplateItemForNonOwnedTemplate() {
        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("non-owned multi-payment template is fetched by id") {
            call(multiPaymentTemplateRepository.getById(TEMPLATE_ID))
                .willReturn(TEMPLATE.copy(userId = UserId(UUID.randomUUID())))
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = mock()
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.deleteMultiPaymentTemplateItem(TEMPLATE_ID, ITEM.id, USER_IDENTIFIER)
            }

            expectInteractions(multiPaymentTemplateRepository) {
                once.getById(TEMPLATE_ID)
            }
        }
    }

    @Test
    fun mustThrowResourceNotFoundExceptionWhenDeletingMultiTemplateItemForNonExistentTemplate() {
        val utcDateTimeProvider = mock<UtcDateTimeProvider>()
        val updateTimestamp = TestData.TIMESTAMP + 1.seconds

        suppose("some timestamp will be returned") {
            call(utcDateTimeProvider.getUtcDateTime())
                .willReturn(updateTimestamp)
        }

        val multiPaymentTemplateRepository = mock<MultiPaymentTemplateRepository>()

        suppose("multi-payment template is fetched by id") {
            call(multiPaymentTemplateRepository.getById(TEMPLATE_ID))
                .willReturn(TEMPLATE)
        }

        suppose("null will be returned when deleting multi-payment template item from the database") {
            call(multiPaymentTemplateRepository.deleteItem(TEMPLATE_ID, ITEM.id, updateTimestamp))
                .willReturn(null)
        }

        val service = MultiPaymentTemplateServiceImpl(
            multiPaymentTemplateRepository = multiPaymentTemplateRepository,
            uuidProvider = mock(),
            utcDateTimeProvider = utcDateTimeProvider
        )

        verify("ResourceNotFoundException is thrown") {
            expectThrows<ResourceNotFoundException> {
                service.deleteMultiPaymentTemplateItem(TEMPLATE_ID, ITEM.id, USER_IDENTIFIER)
            }

            expectInteractions(multiPaymentTemplateRepository) {
                once.getById(TEMPLATE_ID)
                once.deleteItem(TEMPLATE_ID, ITEM.id, updateTimestamp)
            }
        }
    }
}
