package polycode.blockchain

import polycode.blockchain.properties.ChainSpec
import polycode.features.contract.readcall.model.params.ExecuteReadonlyFunctionCallParams
import polycode.features.contract.readcall.model.result.ReadonlyFunctionCallResult
import polycode.features.payout.model.params.GetPayoutsForInvestorParams
import polycode.features.payout.model.result.PayoutForInvestor
import polycode.features.payout.util.PayoutAccountBalance
import polycode.model.DeserializableEvent
import polycode.model.result.BlockchainTransactionInfo
import polycode.model.result.ContractDeploymentTransactionInfo
import polycode.util.AccountBalance
import polycode.util.BlockName
import polycode.util.BlockNumber
import polycode.util.BlockParameter
import polycode.util.ContractAddress
import polycode.util.EthStorageSlot
import polycode.util.TransactionHash
import polycode.util.WalletAddress

interface BlockchainService {
    fun readStorageSlot(
        chainSpec: ChainSpec,
        contractAddress: ContractAddress,
        slot: EthStorageSlot,
        blockParameter: BlockParameter = BlockName.LATEST
    ): String

    fun fetchAccountBalance(
        chainSpec: ChainSpec,
        walletAddress: WalletAddress,
        blockParameter: BlockParameter = BlockName.LATEST
    ): AccountBalance

    fun fetchErc20AccountBalance(
        chainSpec: ChainSpec,
        contractAddress: ContractAddress,
        walletAddress: WalletAddress,
        blockParameter: BlockParameter = BlockName.LATEST
    ): AccountBalance

    fun fetchTransactionInfo(
        chainSpec: ChainSpec,
        txHash: TransactionHash,
        events: List<DeserializableEvent>
    ): BlockchainTransactionInfo?

    fun callReadonlyFunction(
        chainSpec: ChainSpec,
        params: ExecuteReadonlyFunctionCallParams,
        blockParameter: BlockParameter = BlockName.LATEST
    ): ReadonlyFunctionCallResult

    fun findContractDeploymentTransaction(
        chainSpec: ChainSpec,
        contractAddress: ContractAddress,
        events: List<DeserializableEvent>
    ): ContractDeploymentTransactionInfo?

    fun fetchErc20AccountBalances(
        chainSpec: ChainSpec,
        erc20ContractAddress: ContractAddress,
        ignoredErc20Addresses: Set<WalletAddress>,
        startBlock: BlockNumber?,
        endBlock: BlockNumber
    ): List<PayoutAccountBalance>

    fun getPayoutsForInvestor(chainSpec: ChainSpec, params: GetPayoutsForInvestorParams): List<PayoutForInvestor>
}
