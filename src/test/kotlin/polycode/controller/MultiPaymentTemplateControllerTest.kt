package polycode.controller

import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.springframework.http.ResponseEntity
import polycode.JsonSchemaDocumentation
import polycode.TestBase
import polycode.TestData
import polycode.features.api.access.model.result.UserWalletAddressIdentifier
import polycode.features.asset.multisend.controller.MultiPaymentTemplateController
import polycode.features.asset.multisend.model.request.CreateMultiPaymentTemplateRequest
import polycode.features.asset.multisend.model.request.MultiPaymentTemplateItemRequest
import polycode.features.asset.multisend.model.request.UpdateMultiPaymentTemplateRequest
import polycode.features.asset.multisend.model.response.MultiPaymentTemplateWithItemsResponse
import polycode.features.asset.multisend.model.response.MultiPaymentTemplateWithoutItemsResponse
import polycode.features.asset.multisend.model.response.MultiPaymentTemplatesResponse
import polycode.features.asset.multisend.model.result.MultiPaymentTemplate
import polycode.features.asset.multisend.model.result.MultiPaymentTemplateItem
import polycode.features.asset.multisend.model.result.WithItems
import polycode.features.asset.multisend.service.MultiPaymentTemplateService
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

class MultiPaymentTemplateControllerTest : TestBase() {

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
    }

    @Test
    fun mustCorrectlyCreateMultiPaymentTemplate() {
        val request = CreateMultiPaymentTemplateRequest(
            templateName = TEMPLATE.templateName,
            assetType = AssetType.TOKEN,
            tokenAddress = TEMPLATE.tokenAddress?.rawValue,
            chainId = TEMPLATE.chainId.value,
            items = listOf(
                MultiPaymentTemplateItemRequest(
                    walletAddress = ITEM.walletAddress.rawValue,
                    itemName = ITEM.itemName,
                    amount = ITEM.assetAmount.rawValue
                )
            )
        )

        val service = mock<MultiPaymentTemplateService>()

        suppose("multi-payment template will be created") {
            call(service.createMultiPaymentTemplate(request, USER_IDENTIFIER))
                .willReturn(TEMPLATE)
        }

        val controller = MultiPaymentTemplateController(service)

        verify("controller returns correct response") {
            val response = controller.createMultiPaymentTemplate(USER_IDENTIFIER, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(MultiPaymentTemplateWithItemsResponse(TEMPLATE)))
        }
    }

    @Test
    fun mustCorrectlyUpdateMultiPaymentTemplate() {
        val request = UpdateMultiPaymentTemplateRequest(
            templateName = TEMPLATE.templateName,
            assetType = AssetType.TOKEN,
            tokenAddress = TEMPLATE.tokenAddress?.rawValue,
            chainId = TEMPLATE.chainId.value
        )

        val service = mock<MultiPaymentTemplateService>()

        suppose("multi-payment template will be updated") {
            call(service.updateMultiPaymentTemplate(TEMPLATE_ID, request, USER_IDENTIFIER))
                .willReturn(TEMPLATE)
        }

        val controller = MultiPaymentTemplateController(service)

        verify("controller returns correct response") {
            val response = controller.updateMultiPaymentTemplate(TEMPLATE_ID, USER_IDENTIFIER, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(MultiPaymentTemplateWithItemsResponse(TEMPLATE)))
        }
    }

    @Test
    fun mustCorrectlyDeleteMultiPaymentTemplate() {
        val service = mock<MultiPaymentTemplateService>()
        val controller = MultiPaymentTemplateController(service)

        verify("controller returns correct response") {
            controller.deleteMultiPaymentTemplate(TEMPLATE_ID, USER_IDENTIFIER)

            expectInteractions(service) {
                once.deleteMultiPaymentTemplateById(TEMPLATE_ID, USER_IDENTIFIER)
            }
        }
    }

    @Test
    fun mustCorrectlyFetchMultiPaymentTemplateById() {
        val service = mock<MultiPaymentTemplateService>()

        suppose("multi-payment template will be fetched") {
            call(service.getMultiPaymentTemplateById(TEMPLATE_ID))
                .willReturn(TEMPLATE)
        }

        val controller = MultiPaymentTemplateController(service)

        verify("controller returns correct response") {
            val response = controller.getMultiPaymentTemplateById(TEMPLATE_ID)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(MultiPaymentTemplateWithItemsResponse(TEMPLATE)))
        }
    }

    @Test
    fun mustCorrectlyFetchAllMultiPaymentTemplatesByWalletAddress() {
        val service = mock<MultiPaymentTemplateService>()

        suppose("multi-payment template will be fetched") {
            call(service.getAllMultiPaymentTemplatesByWalletAddress(USER_IDENTIFIER.walletAddress))
                .willReturn(listOf(TEMPLATE.withoutItems()))
        }

        val controller = MultiPaymentTemplateController(service)

        verify("controller returns correct response") {
            val response = controller.getAllMultiPaymentTemplatesByWalletAddress(USER_IDENTIFIER.walletAddress.rawValue)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(
                    ResponseEntity.ok(
                        MultiPaymentTemplatesResponse(
                            listOf(
                                MultiPaymentTemplateWithoutItemsResponse(TEMPLATE.withoutItems())
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyAddItemToMultiPaymentTemplate() {
        val request = MultiPaymentTemplateItemRequest(
            walletAddress = ITEM.walletAddress.rawValue,
            itemName = ITEM.itemName,
            amount = ITEM.assetAmount.rawValue
        )

        val service = mock<MultiPaymentTemplateService>()

        suppose("multi-payment template item will be created") {
            call(service.addItemToMultiPaymentTemplate(TEMPLATE_ID, request, USER_IDENTIFIER))
                .willReturn(TEMPLATE)
        }

        val controller = MultiPaymentTemplateController(service)

        verify("controller returns correct response") {
            val response = controller.addItemToMultiPaymentTemplate(TEMPLATE_ID, USER_IDENTIFIER, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(MultiPaymentTemplateWithItemsResponse(TEMPLATE)))
        }
    }

    @Test
    fun mustCorrectlyUpdateMultiPaymentTemplateItem() {
        val request = MultiPaymentTemplateItemRequest(
            walletAddress = ITEM.walletAddress.rawValue,
            itemName = ITEM.itemName,
            amount = ITEM.assetAmount.rawValue
        )

        val service = mock<MultiPaymentTemplateService>()

        suppose("multi-payment template item will be updated") {
            call(service.updateMultiPaymentTemplateItem(TEMPLATE_ID, ITEM.id, request, USER_IDENTIFIER))
                .willReturn(TEMPLATE)
        }

        val controller = MultiPaymentTemplateController(service)

        verify("controller returns correct response") {
            val response = controller.updateMultiPaymentTemplateItem(TEMPLATE_ID, ITEM.id, USER_IDENTIFIER, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(MultiPaymentTemplateWithItemsResponse(TEMPLATE)))
        }
    }

    @Test
    fun mustCorrectlyDeleteMultiPaymentTemplateItem() {
        val service = mock<MultiPaymentTemplateService>()

        suppose("multi-payment template item will be deleted") {
            call(service.deleteMultiPaymentTemplateItem(TEMPLATE_ID, ITEM.id, USER_IDENTIFIER))
                .willReturn(TEMPLATE)
        }

        val controller = MultiPaymentTemplateController(service)

        verify("controller returns correct response") {
            val response = controller.deleteMultiPaymentTemplateItem(TEMPLATE_ID, ITEM.id, USER_IDENTIFIER)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            expectThat(response)
                .isEqualTo(ResponseEntity.ok(MultiPaymentTemplateWithItemsResponse(TEMPLATE)))
        }
    }
}
