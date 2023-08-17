package polycode.features.payout.model.params

import polycode.features.payout.util.MerkleHash
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.WalletAddress

data class FetchMerkleTreePathParams(
    val rootHash: MerkleHash,
    val chainId: ChainId,
    val assetContractAddress: ContractAddress,
    val walletAddress: WalletAddress
) {
    val toFetchMerkleTreeParams: FetchMerkleTreeParams
        get() = FetchMerkleTreeParams(rootHash, chainId, assetContractAddress)
}
