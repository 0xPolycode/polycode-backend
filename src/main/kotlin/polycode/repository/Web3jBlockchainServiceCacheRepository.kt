package polycode.repository

import polycode.blockchain.properties.ChainSpec
import polycode.generated.jooq.id.ContractDeploymentTransactionCacheId
import polycode.generated.jooq.id.FetchAccountBalanceCacheId
import polycode.generated.jooq.id.FetchErc20AccountBalanceCacheId
import polycode.generated.jooq.id.FetchTransactionInfoCacheId
import polycode.model.EventLog
import polycode.model.result.BlockchainTransactionInfo
import polycode.model.result.ContractDeploymentTransactionInfo
import polycode.util.AccountBalance
import polycode.util.BlockNumber
import polycode.util.ContractAddress
import polycode.util.TransactionHash
import polycode.util.WalletAddress

interface Web3jBlockchainServiceCacheRepository {
    fun cacheFetchAccountBalance(id: FetchAccountBalanceCacheId, chainSpec: ChainSpec, accountBalance: AccountBalance)
    fun cacheFetchErc20AccountBalance(
        id: FetchErc20AccountBalanceCacheId,
        chainSpec: ChainSpec,
        contractAddress: ContractAddress,
        accountBalance: AccountBalance
    )

    @Suppress("LongParameterList")
    fun cacheFetchTransactionInfo(
        id: FetchTransactionInfoCacheId,
        chainSpec: ChainSpec,
        txHash: TransactionHash,
        blockNumber: BlockNumber,
        txInfo: BlockchainTransactionInfo,
        eventLogs: List<EventLog>
    )

    fun cacheContractDeploymentTransaction(
        id: ContractDeploymentTransactionCacheId,
        chainSpec: ChainSpec,
        contractAddress: ContractAddress,
        contractDeploymentTransactionInfo: ContractDeploymentTransactionInfo,
        eventLogs: List<EventLog>
    )

    fun getCachedFetchAccountBalance(
        chainSpec: ChainSpec,
        walletAddress: WalletAddress,
        blockNumber: BlockNumber
    ): AccountBalance?

    fun getCachedFetchErc20AccountBalance(
        chainSpec: ChainSpec,
        contractAddress: ContractAddress,
        walletAddress: WalletAddress,
        blockNumber: BlockNumber
    ): AccountBalance?

    fun getCachedFetchTransactionInfo(
        chainSpec: ChainSpec,
        txHash: TransactionHash,
        currentBlockNumber: BlockNumber
    ): Pair<BlockchainTransactionInfo, List<EventLog>>?

    fun getCachedContractDeploymentTransaction(
        chainSpec: ChainSpec,
        contractAddress: ContractAddress,
    ): Pair<ContractDeploymentTransactionInfo, List<EventLog>>?
}
