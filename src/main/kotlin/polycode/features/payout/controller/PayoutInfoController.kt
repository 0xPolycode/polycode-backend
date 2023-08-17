package polycode.features.payout.controller

import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import polycode.config.validation.ValidEthAddress
import polycode.exception.ResourceNotFoundException
import polycode.features.payout.model.params.FetchMerkleTreeParams
import polycode.features.payout.model.params.FetchMerkleTreePathParams
import polycode.features.payout.model.response.FetchMerkleTreePathResponse
import polycode.features.payout.model.response.FetchMerkleTreeResponse
import polycode.features.payout.repository.MerkleTreeRepository
import polycode.features.payout.util.MerkleHash
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.WalletAddress

@Validated
@RestController
class PayoutInfoController(private val merkleTreeRepository: MerkleTreeRepository) {

    @GetMapping("/v1/payout-info/{chainId}/{assetContractAddress}/tree/{rootHash}")
    fun getPayoutTree(
        @PathVariable chainId: Long,
        @ValidEthAddress @PathVariable assetContractAddress: String,
        @PathVariable rootHash: String
    ): ResponseEntity<FetchMerkleTreeResponse> {
        val params = FetchMerkleTreeParams(
            rootHash = MerkleHash(rootHash),
            chainId = ChainId(chainId),
            assetContractAddress = ContractAddress(assetContractAddress)
        )

        val tree = merkleTreeRepository.fetchTree(params)?.tree
            ?: throw ResourceNotFoundException("Payout does not exist for specified parameters")

        return ResponseEntity.ok(FetchMerkleTreeResponse(tree))
    }

    @Suppress("ThrowsCount")
    @GetMapping("/v1/payout-info/{chainId}/{assetContractAddress}/tree/{rootHash}/path/{walletAddress}")
    fun getPayoutPath(
        @PathVariable chainId: Long,
        @ValidEthAddress @PathVariable assetContractAddress: String,
        @PathVariable rootHash: String,
        @ValidEthAddress @PathVariable walletAddress: String
    ): ResponseEntity<FetchMerkleTreePathResponse> {
        val params = FetchMerkleTreePathParams(
            rootHash = MerkleHash(rootHash),
            chainId = ChainId(chainId),
            assetContractAddress = ContractAddress(assetContractAddress),
            walletAddress = WalletAddress(walletAddress)
        )

        val payoutExists = merkleTreeRepository.containsAddress(params)

        if (payoutExists.not()) {
            throw ResourceNotFoundException(
                "Payout does not exist for specified parameters or account is not included in payout"
            )
        }

        val tree = merkleTreeRepository.fetchTree(params.toFetchMerkleTreeParams)?.tree
            ?: throw ResourceNotFoundException("Payout does not exist for specified parameters")

        val (path, accountBalance) = tree.leafNodesByAddress[params.walletAddress]?.let {
            tree.pathTo(it.value.data)?.let { path -> Pair(path, it.value.data) }
        }
            ?: throw ResourceNotFoundException("Account is not included in payout")

        return ResponseEntity.ok(
            FetchMerkleTreePathResponse(
                walletAddress = accountBalance.address.rawValue,
                walletBalance = accountBalance.balance.rawValue,
                path = path
            )
        )
    }
}
