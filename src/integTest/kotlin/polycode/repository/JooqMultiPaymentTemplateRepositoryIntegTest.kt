package polycode.repository

import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import polycode.TestBase
import polycode.TestData
import polycode.config.DatabaseConfig
import polycode.features.asset.multisend.model.result.MultiPaymentTemplate
import polycode.features.asset.multisend.model.result.MultiPaymentTemplateItem
import polycode.features.asset.multisend.model.result.WithItems
import polycode.features.asset.multisend.repository.JooqMultiPaymentTemplateRepository
import polycode.generated.jooq.enums.UserIdentifierType
import polycode.generated.jooq.id.MultiPaymentTemplateId
import polycode.generated.jooq.id.MultiPaymentTemplateItemId
import polycode.generated.jooq.id.UserId
import polycode.generated.jooq.tables.records.UserIdentifierRecord
import polycode.testcontainers.SharedTestContainers
import polycode.util.Balance
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.WalletAddress
import java.math.BigInteger
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@JooqTest
@Import(JooqMultiPaymentTemplateRepository::class, DatabaseConfig::class)
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqMultiPaymentTemplateRepositoryIntegTest : TestBase() {

    companion object {
        private val TEMPLATE_ID = MultiPaymentTemplateId(UUID.randomUUID())
        private const val TEMPLATE_NAME = "templateName"
        private val CHAIN_ID = ChainId(1337L)
        private val WALLET_ADDRESS = WalletAddress("a")
        private const val ITEM_NAME = "itemName"
        private val TOKEN_ADDRESS = ContractAddress("b")
        private val ASSET_AMOUNT = Balance(BigInteger.TEN)
        private val OWNER_ID = UserId(UUID.randomUUID())
        private val OWNER_ADDRESS = WalletAddress("cafebabe")
        private val TEMPLATE = MultiPaymentTemplate(
            id = TEMPLATE_ID,
            items = WithItems(
                listOf(
                    MultiPaymentTemplateItem(
                        id = MultiPaymentTemplateItemId(UUID.randomUUID()),
                        templateId = TEMPLATE_ID,
                        walletAddress = WALLET_ADDRESS,
                        itemName = ITEM_NAME,
                        assetAmount = ASSET_AMOUNT,
                        createdAt = TestData.TIMESTAMP
                    )
                )
            ),
            templateName = TEMPLATE_NAME,
            tokenAddress = TOKEN_ADDRESS,
            chainId = CHAIN_ID,
            userId = OWNER_ID,
            createdAt = TestData.TIMESTAMP,
            updatedAt = null
        )
    }

    @Suppress("unused")
    private val postgresContainer = SharedTestContainers.postgresContainer

    @Autowired
    private lateinit var repository: JooqMultiPaymentTemplateRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)

        dslContext.executeInsert(
            UserIdentifierRecord(
                id = OWNER_ID,
                userIdentifier = OWNER_ADDRESS.rawValue,
                identifierType = UserIdentifierType.ETH_WALLET_ADDRESS
            )
        )
    }

    @Test
    fun mustCorrectlyStoreMultiPaymentTemplate() {
        val storedTemplate = suppose("multi-payment template is stored in database") {
            repository.store(TEMPLATE)
        }

        verify("storing multi-payment template returns correct result") {
            expectThat(storedTemplate)
                .isEqualTo(TEMPLATE)
        }

        verify("multi-payment template is stored into the database") {
            expectThat(repository.getById(TEMPLATE_ID))
                .isEqualTo(TEMPLATE)
        }
    }

    @Test
    fun mustCorrectlyUpdateMultiPaymentTemplate() {
        suppose("multi-payment template is stored in database") {
            repository.store(TEMPLATE)
        }

        val templateUpdate = TEMPLATE.copy(
            templateName = "newName",
            tokenAddress = null,
            chainId = ChainId(123L),
            updatedAt = TestData.TIMESTAMP
        )

        val updatedTemplate = suppose("multi-payment template is updated in database") {
            repository.update(templateUpdate)
        }

        verify("updating template returns correct result") {
            expectThat(updatedTemplate)
                .isEqualTo(templateUpdate)
        }

        verify("template is updated in the database") {
            expectThat(repository.getById(TEMPLATE_ID))
                .isEqualTo(templateUpdate)
        }
    }

    @Test
    fun mustReturnNullWhenUpdatingNonExistentMultiPaymentTemplate() {
        verify("null is returned when updating non-existent multi-payment template") {
            expectThat(repository.update(TEMPLATE))
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyDeleteMultiPaymentTemplate() {
        suppose("multi-payment template is stored in database") {
            repository.store(TEMPLATE)
        }

        verify("multi-payment template is stored into the database") {
            expectThat(repository.getById(TEMPLATE_ID))
                .isEqualTo(TEMPLATE)
        }

        suppose("multi-payment template is deleted from the database") {
            repository.delete(TEMPLATE_ID)
        }

        verify("multi-payment template was deleted from the database") {
            expectThat(repository.getById(TEMPLATE_ID))
                .isNull()
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentMultiPaymentTemplateById() {
        verify("null is returned for non-existent multi-payment template") {
            expectThat(repository.getById(TEMPLATE_ID))
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyFetchMultiPaymentTemplateItemsById() {
        suppose("multi-payment template is stored in database") {
            repository.store(TEMPLATE)
        }

        verify("correct multi-payment template items are returned") {
            expectThat(repository.getItemsById(TEMPLATE_ID))
                .isEqualTo(TEMPLATE.items.value)
        }
    }

    @Test
    fun mustCorrectlyFetchAllMultiPaymentTemplatesByWalletAddress() {
        suppose("multi-payment template is stored in database") {
            repository.store(TEMPLATE)
        }

        verify("multi-payment templates are correctly fetched by wallet address") {
            expectThat(repository.getAllByWalletAddress(OWNER_ADDRESS))
                .isEqualTo(listOf(TEMPLATE.withoutItems()))
        }
    }

    @Test
    fun mustCorrectlyAddItemToMultiPaymentTemplate() {
        suppose("multi-payment template is stored in database") {
            repository.store(TEMPLATE)
        }

        val newItem = MultiPaymentTemplateItem(
            id = MultiPaymentTemplateItemId(UUID.randomUUID()),
            templateId = TEMPLATE_ID,
            walletAddress = WalletAddress("abc"),
            itemName = "newItemName",
            assetAmount = Balance(BigInteger.TWO),
            createdAt = TestData.TIMESTAMP + 1.seconds
        )

        val templateWithAddedItem = suppose("item is added to multi-payment template") {
            repository.addItem(newItem, TestData.TIMESTAMP)
        }

        verify("item was added to multi-payment template") {
            expectThat(templateWithAddedItem)
                .isEqualTo(
                    TEMPLATE.copy(
                        items = WithItems(TEMPLATE.items.value + newItem),
                        updatedAt = TestData.TIMESTAMP
                    )
                )
        }

        verify("item was added to the database") {
            expectThat(repository.getById(TEMPLATE_ID))
                .isEqualTo(templateWithAddedItem)
        }
    }

    @Test
    fun mustReturnNullWhenAddingItemToNonExistentMultiPaymentTemplate() {
        val item = MultiPaymentTemplateItem(
            id = MultiPaymentTemplateItemId(UUID.randomUUID()),
            templateId = TEMPLATE_ID,
            walletAddress = WalletAddress("abc"),
            itemName = "newItemName",
            assetAmount = Balance(BigInteger.TWO),
            createdAt = TestData.TIMESTAMP + 1.seconds
        )

        verify("null is returned when adding item to non-existent multi-payment template") {
            expectThat(repository.addItem(item, TestData.TIMESTAMP))
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyUpdateMultiPaymentTemplateItem() {
        suppose("multi-payment template is stored in database") {
            repository.store(TEMPLATE)
        }

        val updatedItem = TEMPLATE.items.value[0].copy(
            walletAddress = WalletAddress("fff"),
            itemName = "updatedItem",
            assetAmount = Balance(BigInteger.ONE)
        )

        val templateWithUpdatedItem = suppose("multi-payment template item is updated in database") {
            repository.updateItem(updatedItem, TestData.TIMESTAMP)
        }

        verify("item was updated in multi-payment tempalte") {
            expectThat(templateWithUpdatedItem)
                .isEqualTo(
                    TEMPLATE.copy(
                        items = WithItems(listOf(updatedItem)),
                        updatedAt = TestData.TIMESTAMP
                    )
                )
        }

        verify("item was updated in the database") {
            expectThat(repository.getById(TEMPLATE_ID))
                .isEqualTo(templateWithUpdatedItem)
        }
    }

    @Test
    fun mustReturnNullWhenUpdatingItemForNonExistentMultiPaymentTemplate() {
        val updatedItem = TEMPLATE.items.value[0].copy(
            walletAddress = WalletAddress("fff"),
            itemName = "updatedItem",
            assetAmount = Balance(BigInteger.ONE)
        )

        verify("null is returned when updating item for non-existent multi-payment template") {
            expectThat(repository.updateItem(updatedItem, TestData.TIMESTAMP))
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyDeleteMultiPaymentTemplateItem() {
        suppose("multi-payment template is stored in database") {
            repository.store(TEMPLATE)
        }

        val templateWithDeletedItem = suppose("item is deleted from the database") {
            repository.deleteItem(TEMPLATE_ID, TEMPLATE.items.value[0].id, TestData.TIMESTAMP)
        }

        verify("item was deleted from multi-payment template") {
            expectThat(templateWithDeletedItem)
                .isEqualTo(
                    TEMPLATE.copy(
                        items = WithItems(emptyList()),
                        updatedAt = TestData.TIMESTAMP
                    )
                )
        }

        verify("item was deleted from the database") {
            expectThat(repository.getById(TEMPLATE_ID))
                .isEqualTo(templateWithDeletedItem)
        }
    }

    @Test
    fun mustReturnNullWhenDeletingItemFromNonExistentMultiPaymentTemplate() {
        verify("null is returned when deleting item from non-existent multi-payment template") {
            expectThat(
                repository.deleteItem(
                    templateId = MultiPaymentTemplateId(UUID.randomUUID()),
                    itemId = MultiPaymentTemplateItemId(UUID.randomUUID()),
                    updatedAt = TestData.TIMESTAMP
                )
            )
                .isNull()
        }
    }
}
