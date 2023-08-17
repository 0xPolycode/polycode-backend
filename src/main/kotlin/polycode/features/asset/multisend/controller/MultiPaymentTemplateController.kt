package polycode.features.asset.multisend.controller

import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import polycode.config.binding.annotation.UserIdentifierBinding
import polycode.config.validation.ValidEthAddress
import polycode.features.api.access.model.result.UserIdentifier
import polycode.features.asset.multisend.model.request.CreateMultiPaymentTemplateRequest
import polycode.features.asset.multisend.model.request.MultiPaymentTemplateItemRequest
import polycode.features.asset.multisend.model.request.UpdateMultiPaymentTemplateRequest
import polycode.features.asset.multisend.model.response.MultiPaymentTemplateWithItemsResponse
import polycode.features.asset.multisend.model.response.MultiPaymentTemplateWithoutItemsResponse
import polycode.features.asset.multisend.model.response.MultiPaymentTemplatesResponse
import polycode.features.asset.multisend.service.MultiPaymentTemplateService
import polycode.generated.jooq.id.MultiPaymentTemplateId
import polycode.generated.jooq.id.MultiPaymentTemplateItemId
import polycode.util.WalletAddress
import javax.validation.Valid

@Validated
@RestController
class MultiPaymentTemplateController(private val multiPaymentTemplateService: MultiPaymentTemplateService) {

    @PostMapping("/v1/multi-payment-template")
    fun createMultiPaymentTemplate(
        @UserIdentifierBinding userIdentifier: UserIdentifier,
        @Valid @RequestBody requestBody: CreateMultiPaymentTemplateRequest
    ): ResponseEntity<MultiPaymentTemplateWithItemsResponse> {
        return ResponseEntity.ok(
            MultiPaymentTemplateWithItemsResponse(
                multiPaymentTemplateService.createMultiPaymentTemplate(
                    request = requestBody,
                    userIdentifier = userIdentifier
                )
            )
        )
    }

    @PatchMapping("/v1/multi-payment-template/{id}")
    fun updateMultiPaymentTemplate(
        @PathVariable("id") id: MultiPaymentTemplateId,
        @UserIdentifierBinding userIdentifier: UserIdentifier,
        @Valid @RequestBody requestBody: UpdateMultiPaymentTemplateRequest
    ): ResponseEntity<MultiPaymentTemplateWithItemsResponse> {
        return ResponseEntity.ok(
            MultiPaymentTemplateWithItemsResponse(
                multiPaymentTemplateService.updateMultiPaymentTemplate(
                    templateId = id,
                    request = requestBody,
                    userIdentifier = userIdentifier
                )
            )
        )
    }

    @DeleteMapping("/v1/multi-payment-template/{id}")
    fun deleteMultiPaymentTemplate(
        @PathVariable("id") id: MultiPaymentTemplateId,
        @UserIdentifierBinding userIdentifier: UserIdentifier
    ) {
        multiPaymentTemplateService.deleteMultiPaymentTemplateById(
            templateId = id,
            userIdentifier = userIdentifier
        )
    }

    @GetMapping("/v1/multi-payment-template/{id}")
    fun getMultiPaymentTemplateById(
        @PathVariable("id") id: MultiPaymentTemplateId
    ): ResponseEntity<MultiPaymentTemplateWithItemsResponse> {
        return ResponseEntity.ok(
            MultiPaymentTemplateWithItemsResponse(
                multiPaymentTemplateService.getMultiPaymentTemplateById(id)
            )
        )
    }

    @GetMapping("/v1/multi-payment-template/by-wallet-address/{walletAddress}")
    fun getAllMultiPaymentTemplatesByWalletAddress(
        @ValidEthAddress @PathVariable("walletAddress") walletAddress: String
    ): ResponseEntity<MultiPaymentTemplatesResponse> {
        return ResponseEntity.ok(
            MultiPaymentTemplatesResponse(
                multiPaymentTemplateService.getAllMultiPaymentTemplatesByWalletAddress(WalletAddress(walletAddress))
                    .map { MultiPaymentTemplateWithoutItemsResponse(it) }
            )
        )
    }

    @PostMapping("/v1/multi-payment-template/{templateId}/items")
    fun addItemToMultiPaymentTemplate(
        @PathVariable("templateId") templateId: MultiPaymentTemplateId,
        @UserIdentifierBinding userIdentifier: UserIdentifier,
        @Valid @RequestBody requestBody: MultiPaymentTemplateItemRequest
    ): ResponseEntity<MultiPaymentTemplateWithItemsResponse> {
        return ResponseEntity.ok(
            MultiPaymentTemplateWithItemsResponse(
                multiPaymentTemplateService.addItemToMultiPaymentTemplate(
                    templateId = templateId,
                    request = requestBody,
                    userIdentifier = userIdentifier
                )
            )
        )
    }

    @PatchMapping("/v1/multi-payment-template/{templateId}/items/{itemId}")
    fun updateMultiPaymentTemplateItem(
        @PathVariable("templateId") templateId: MultiPaymentTemplateId,
        @PathVariable("itemId") itemId: MultiPaymentTemplateItemId,
        @UserIdentifierBinding userIdentifier: UserIdentifier,
        @Valid @RequestBody requestBody: MultiPaymentTemplateItemRequest
    ): ResponseEntity<MultiPaymentTemplateWithItemsResponse> {
        return ResponseEntity.ok(
            MultiPaymentTemplateWithItemsResponse(
                multiPaymentTemplateService.updateMultiPaymentTemplateItem(
                    templateId = templateId,
                    itemId = itemId,
                    request = requestBody,
                    userIdentifier = userIdentifier
                )
            )
        )
    }

    @DeleteMapping("/v1/multi-payment-template/{templateId}/items/{itemId}")
    fun deleteMultiPaymentTemplateItem(
        @PathVariable("templateId") templateId: MultiPaymentTemplateId,
        @PathVariable("itemId") itemId: MultiPaymentTemplateItemId,
        @UserIdentifierBinding userIdentifier: UserIdentifier
    ): ResponseEntity<MultiPaymentTemplateWithItemsResponse> {
        return ResponseEntity.ok(
            MultiPaymentTemplateWithItemsResponse(
                multiPaymentTemplateService.deleteMultiPaymentTemplateItem(
                    templateId = templateId,
                    itemId = itemId,
                    userIdentifier = userIdentifier
                )
            )
        )
    }
}
