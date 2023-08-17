package polycode.features.payout.controller

import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import polycode.blockchain.BlockchainService
import polycode.blockchain.properties.ChainSpec
import polycode.config.binding.annotation.UserIdentifierBinding
import polycode.config.validation.ValidEthAddress
import polycode.features.api.access.model.result.UserIdentifier
import polycode.features.api.access.model.result.UserWalletAddressIdentifier
import polycode.features.payout.model.params.FetchMerkleTreeParams
import polycode.features.payout.model.params.GetPayoutsForInvestorParams
import polycode.features.payout.model.response.InvestorPayoutResponse
import polycode.features.payout.model.response.InvestorPayoutsResponse
import polycode.features.payout.repository.MerkleTreeRepository
import polycode.util.ChainId
import polycode.util.ContractAddress

@Validated
@RestController
class ClaimablePayoutsController(
    private val blockchainService: BlockchainService,
    private val merkleTreeRepository: MerkleTreeRepository
) {

    @Suppress("LongParameterList")
    @GetMapping("/v1/claimable-payouts")
    fun getPayoutsForInvestor(
        @RequestParam(required = true) chainId: Long,
        @ValidEthAddress @RequestParam(required = true) payoutManager: String,
        @UserIdentifierBinding userIdentifier: UserIdentifier
    ): ResponseEntity<InvestorPayoutsResponse> {
        val chainIdValue = ChainId(chainId)
        val payouts = (userIdentifier as? UserWalletAddressIdentifier)?.walletAddress?.let {
            blockchainService.getPayoutsForInvestor(
                chainSpec = ChainSpec(chainIdValue, null),
                GetPayoutsForInvestorParams(
                    payoutManager = ContractAddress(payoutManager),
                    investor = it
                )
            )
        }.orEmpty()

        val merkleTreeParams = payouts.mapTo(HashSet()) {
            FetchMerkleTreeParams(it.payout.assetSnapshotMerkleRoot, chainIdValue, it.payout.asset)
        }
        val merkleTrees = merkleTreeParams.mapNotNull { merkleTreeRepository.fetchTree(it)?.tree }
            .associateBy { it.root.hash }

        val investorPayouts = payouts.mapNotNull { payoutData ->
            val tree = merkleTrees[payoutData.payout.assetSnapshotMerkleRoot]
            val accountBalance = tree?.leafNodesByAddress?.get(payoutData.investor)?.value?.data
            val path = accountBalance?.let { tree.pathTo(it) }

            if (path != null) { // return only claimable (and already claimed) payouts for this investor
                val totalRewardAmount = payoutData.payout.totalRewardAmount.rawValue
                val balance = accountBalance.balance.rawValue
                val totalAssetAmount = payoutData.payout.totalAssetAmount.rawValue
                val totalAmountClaimable = (totalRewardAmount * balance) / totalAssetAmount
                val amountClaimable = totalAmountClaimable - payoutData.amountClaimed.rawValue

                val payout = payoutData.payout.toPayoutResponse()

                InvestorPayoutResponse(
                    payout = payout,
                    investor = payoutData.investor.rawValue,
                    amountClaimed = payoutData.amountClaimed.rawValue,

                    amountClaimable = amountClaimable,
                    balance = accountBalance.balance.rawValue,
                    path = path
                )
            } else null
        }

        return ResponseEntity.ok(InvestorPayoutsResponse(investorPayouts))
    }
}
