package polycode.features.blacklist.controller

import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import polycode.config.binding.annotation.UserIdentifierBinding
import polycode.config.validation.ValidEthAddress
import polycode.features.api.access.model.result.UserIdentifier
import polycode.features.blacklist.model.response.BlacklistedAddressesResponse
import polycode.features.blacklist.service.BlacklistService
import polycode.util.WalletAddress

@Validated
@RestController
class BlacklistController(private val blacklistService: BlacklistService) {

    @PostMapping("/v1/blacklist/{address}")
    fun addAddress(
        @UserIdentifierBinding userIdentifier: UserIdentifier,
        @ValidEthAddress @PathVariable address: String
    ) {
        blacklistService.addAddress(userIdentifier, WalletAddress(address))
    }

    @DeleteMapping("/v1/blacklist/{address}")
    fun removeAddress(
        @UserIdentifierBinding userIdentifier: UserIdentifier,
        @ValidEthAddress @PathVariable address: String
    ) {
        blacklistService.removeAddress(userIdentifier, WalletAddress(address))
    }

    @GetMapping("/v1/blacklist")
    fun listAddresses(
        @UserIdentifierBinding userIdentifier: UserIdentifier
    ): ResponseEntity<BlacklistedAddressesResponse> =
        ResponseEntity.ok(
            BlacklistedAddressesResponse(
                blacklistService.listAddresses(userIdentifier).map { it.rawValue }
            )
        )
}
