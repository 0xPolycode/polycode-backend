package polycode.repository

import mu.KLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Repository
import polycode.blockchain.properties.ChainSpec
import polycode.generated.jooq.id.ContractDeploymentTransactionCacheId
import polycode.generated.jooq.id.FetchAccountBalanceCacheId
import polycode.generated.jooq.id.FetchErc20AccountBalanceCacheId
import polycode.generated.jooq.id.FetchTransactionInfoCacheId
import polycode.generated.jooq.tables.ContractDeploymentTransactionCacheTable
import polycode.generated.jooq.tables.FetchAccountBalanceCacheTable
import polycode.generated.jooq.tables.FetchErc20AccountBalanceCacheTable
import polycode.generated.jooq.tables.FetchTransactionInfoCacheTable
import polycode.generated.jooq.tables.records.ContractDeploymentTransactionCacheRecord
import polycode.generated.jooq.tables.records.FetchAccountBalanceCacheRecord
import polycode.generated.jooq.tables.records.FetchErc20AccountBalanceCacheRecord
import polycode.generated.jooq.tables.records.FetchTransactionInfoCacheRecord
import polycode.generated.jooq.udt.records.EventLogRecord
import polycode.model.EventLog
import polycode.model.result.BlockchainTransactionInfo
import polycode.model.result.ContractBinaryInfo
import polycode.model.result.ContractDeploymentTransactionInfo
import polycode.model.result.FullContractDeploymentTransactionInfo
import polycode.util.AccountBalance
import polycode.util.BlockNumber
import polycode.util.ContractAddress
import polycode.util.ContractBinaryData
import polycode.util.TransactionHash
import polycode.util.WalletAddress
import java.math.BigInteger

@Repository
class JooqWeb3jBlockchainServiceCacheRepository(private val dslContext: DSLContext) :
    Web3jBlockchainServiceCacheRepository {

    companion object : KLogging()

    override fun cacheFetchAccountBalance(
        id: FetchAccountBalanceCacheId,
        chainSpec: ChainSpec,
        accountBalance: AccountBalance
    ) {
        logger.info {
            "Caching fetchAccountBalance call, id: $id, chainSpec: $chainSpec, accountBalance: $accountBalance"
        }

        try {
            dslContext.executeInsert(
                FetchAccountBalanceCacheRecord(
                    id = id,
                    chainId = chainSpec.chainId,
                    customRpcUrl = chainSpec.customRpcUrl ?: "",
                    walletAddress = accountBalance.wallet,
                    blockNumber = accountBalance.blockNumber,
                    timestamp = accountBalance.timestamp,
                    assetAmount = accountBalance.amount
                )
            )
        } catch (_: DuplicateKeyException) {
            logger.info {
                "Already cached fetchAccountBalance call, id: $id, chainSpec: $chainSpec," +
                    " accountBalance: $accountBalance"
            }
        }
    }

    override fun cacheFetchErc20AccountBalance(
        id: FetchErc20AccountBalanceCacheId,
        chainSpec: ChainSpec,
        contractAddress: ContractAddress,
        accountBalance: AccountBalance
    ) {
        logger.info {
            "Caching fetchErc20AccountBalance call, id: $id, chainSpec: $chainSpec," +
                " contractAddress: $contractAddress, accountBalance: $accountBalance"
        }

        try {
            dslContext.executeInsert(
                FetchErc20AccountBalanceCacheRecord(
                    id = id,
                    chainId = chainSpec.chainId,
                    customRpcUrl = chainSpec.customRpcUrl ?: "",
                    contractAddress = contractAddress,
                    walletAddress = accountBalance.wallet,
                    blockNumber = accountBalance.blockNumber,
                    timestamp = accountBalance.timestamp,
                    assetAmount = accountBalance.amount
                )
            )
        } catch (_: DuplicateKeyException) {
            logger.info {
                "Already cached fetchErc20AccountBalance call, id: $id, chainSpec: $chainSpec," +
                    " contractAddress: $contractAddress, accountBalance: $accountBalance"
            }
        }
    }

    override fun cacheFetchTransactionInfo(
        id: FetchTransactionInfoCacheId,
        chainSpec: ChainSpec,
        txHash: TransactionHash,
        blockNumber: BlockNumber,
        txInfo: BlockchainTransactionInfo,
        eventLogs: List<EventLog>
    ) {
        logger.info {
            "Caching fetchTransactionInfo call, id: $id, chainSpec: $chainSpec, txHash: $txHash," +
                " blockNumber: $blockNumber"
        }

        try {
            dslContext.executeInsert(
                FetchTransactionInfoCacheRecord(
                    id = id,
                    chainId = chainSpec.chainId,
                    customRpcUrl = chainSpec.customRpcUrl ?: "",
                    txHash = txHash,
                    fromAddress = txInfo.from,
                    toAddress = txInfo.to.toWalletAddress(),
                    deployedContractAddress = txInfo.deployedContractAddress,
                    txData = txInfo.data,
                    valueAmount = txInfo.value,
                    blockNumber = blockNumber,
                    timestamp = txInfo.timestamp,
                    success = txInfo.success,
                    eventLogs = eventLogs.map {
                        EventLogRecord(
                            logData = it.data,
                            logTopics = it.topics.toTypedArray()
                        )
                    }.toTypedArray()
                )
            )
        } catch (_: DuplicateKeyException) {
            logger.info {
                "Already cached fetchTransactionInfo call, id: $id, chainSpec: $chainSpec, txHash: $txHash," +
                    " blockNumber: $blockNumber"
            }
        }
    }

    override fun cacheContractDeploymentTransaction(
        id: ContractDeploymentTransactionCacheId,
        chainSpec: ChainSpec,
        contractAddress: ContractAddress,
        contractDeploymentTransactionInfo: ContractDeploymentTransactionInfo,
        eventLogs: List<EventLog>
    ) {
        logger.info {
            "Caching findContractDeploymentTransaction call, id: $id, chainSpec: $chainSpec," +
                " contractAddress: $contractAddress, contractDeploymentTransactionInfo:" +
                " $contractDeploymentTransactionInfo, eventLogs: $eventLogs"
        }

        val fullTransactionInfo = contractDeploymentTransactionInfo as? FullContractDeploymentTransactionInfo

        try {
            dslContext.executeInsert(
                ContractDeploymentTransactionCacheRecord(
                    id = id,
                    chainId = chainSpec.chainId,
                    customRpcUrl = chainSpec.customRpcUrl ?: "",
                    contractAddress = contractAddress,
                    txHash = fullTransactionInfo?.hash,
                    fromAddress = fullTransactionInfo?.from,
                    txData = fullTransactionInfo?.data,
                    valueAmount = fullTransactionInfo?.value,
                    contractBinary = contractDeploymentTransactionInfo.binary.binary,
                    blockNumber = fullTransactionInfo?.blockNumber,
                    eventLogs = eventLogs.map {
                        EventLogRecord(
                            logData = it.data,
                            logTopics = it.topics.toTypedArray()
                        )
                    }.toTypedArray()
                )
            )
        } catch (_: DuplicateKeyException) {
            logger.info {
                "Already cached findContractDeploymentTransaction call, id: $id, chainSpec: $chainSpec," +
                    " contractAddress: $contractAddress, contractDeploymentTransactionInfo:" +
                    " $contractDeploymentTransactionInfo, eventLogs: $eventLogs"
            }
        }
    }

    override fun getCachedFetchAccountBalance(
        chainSpec: ChainSpec,
        walletAddress: WalletAddress,
        blockNumber: BlockNumber
    ): AccountBalance? {
        logger.debug {
            "Get cached fetchAccountBalance call, chainSpec: $chainSpec, walletAddress: $walletAddress," +
                " blockNumber: $blockNumber"
        }

        return dslContext.selectFrom(FetchAccountBalanceCacheTable)
            .where(
                DSL.and(
                    FetchAccountBalanceCacheTable.CHAIN_ID.eq(chainSpec.chainId),
                    FetchAccountBalanceCacheTable.CUSTOM_RPC_URL.eq(chainSpec.customRpcUrl ?: ""),
                    FetchAccountBalanceCacheTable.WALLET_ADDRESS.eq(walletAddress),
                    FetchAccountBalanceCacheTable.BLOCK_NUMBER.eq(blockNumber)
                )
            )
            .fetchOne()
            ?.let {
                AccountBalance(
                    wallet = it.walletAddress,
                    blockNumber = it.blockNumber,
                    timestamp = it.timestamp,
                    amount = it.assetAmount
                )
            }
    }

    override fun getCachedFetchErc20AccountBalance(
        chainSpec: ChainSpec,
        contractAddress: ContractAddress,
        walletAddress: WalletAddress,
        blockNumber: BlockNumber
    ): AccountBalance? {
        logger.debug {
            "Get cached fetchErc20AccountBalance call, chainSpec: $chainSpec, contractAddress: $contractAddress," +
                " walletAddress: $walletAddress, blockNumber: $blockNumber"
        }

        return dslContext.selectFrom(FetchErc20AccountBalanceCacheTable)
            .where(
                DSL.and(
                    FetchErc20AccountBalanceCacheTable.CHAIN_ID.eq(chainSpec.chainId),
                    FetchErc20AccountBalanceCacheTable.CUSTOM_RPC_URL.eq(chainSpec.customRpcUrl ?: ""),
                    FetchErc20AccountBalanceCacheTable.CONTRACT_ADDRESS.eq(contractAddress),
                    FetchErc20AccountBalanceCacheTable.WALLET_ADDRESS.eq(walletAddress),
                    FetchErc20AccountBalanceCacheTable.BLOCK_NUMBER.eq(blockNumber)
                )
            )
            .fetchOne()
            ?.let {
                AccountBalance(
                    wallet = it.walletAddress,
                    blockNumber = it.blockNumber,
                    timestamp = it.timestamp,
                    amount = it.assetAmount
                )
            }
    }

    override fun getCachedFetchTransactionInfo(
        chainSpec: ChainSpec,
        txHash: TransactionHash,
        currentBlockNumber: BlockNumber
    ): Pair<BlockchainTransactionInfo, List<EventLog>>? {
        logger.debug {
            "Get cached fetchTransactionInfo call, chainSpec: $chainSpec, txHash: $txHash," +
                " currentBlockNumber: $currentBlockNumber"
        }

        return dslContext.selectFrom(FetchTransactionInfoCacheTable)
            .where(
                DSL.and(
                    FetchTransactionInfoCacheTable.CHAIN_ID.eq(chainSpec.chainId),
                    FetchTransactionInfoCacheTable.CUSTOM_RPC_URL.eq(chainSpec.customRpcUrl ?: ""),
                    FetchTransactionInfoCacheTable.TX_HASH.eq(txHash)
                )
            )
            .fetchOne()
            ?.let {
                Pair(
                    BlockchainTransactionInfo(
                        hash = it.txHash,
                        from = it.fromAddress,
                        to = it.toAddress,
                        deployedContractAddress = it.deployedContractAddress,
                        data = it.txData,
                        value = it.valueAmount,
                        blockConfirmations = (currentBlockNumber.value - it.blockNumber.value).max(BigInteger.ZERO),
                        timestamp = it.timestamp,
                        success = it.success,
                        events = emptyList()
                    ),
                    it.eventLogs.map { l ->
                        EventLog(
                            data = l.logData ?: "",
                            topics = l.logTopics?.filterNotNull()?.toList().orEmpty()
                        )
                    }
                )
            }
    }

    override fun getCachedContractDeploymentTransaction(
        chainSpec: ChainSpec,
        contractAddress: ContractAddress
    ): Pair<ContractDeploymentTransactionInfo, List<EventLog>>? {
        logger.debug {
            "Get cached findContractDeploymentTransaction call, chainSpec: $chainSpec," +
                " contractAddress: $contractAddress"
        }

        return dslContext.selectFrom(ContractDeploymentTransactionCacheTable)
            .where(
                DSL.and(
                    ContractDeploymentTransactionCacheTable.CHAIN_ID.eq(chainSpec.chainId),
                    ContractDeploymentTransactionCacheTable.CUSTOM_RPC_URL.eq(chainSpec.customRpcUrl ?: ""),
                    ContractDeploymentTransactionCacheTable.CONTRACT_ADDRESS.eq(contractAddress)
                )
            )
            .fetchOne()
            ?.let {
                Pair(
                    it.toModel(),
                    it.eventLogs.map { l ->
                        EventLog(
                            data = l.logData ?: "",
                            topics = l.logTopics?.filterNotNull()?.toList().orEmpty()
                        )
                    }
                )
            }
    }

    @Suppress("ComplexCondition") // needed to get non-null check
    private fun ContractDeploymentTransactionCacheRecord.toModel(): ContractDeploymentTransactionInfo {
        val hash = txHash
        val from = fromAddress
        val data = txData
        val value = valueAmount
        val binary = ContractBinaryData(contractBinary)
        val blockNumber = this.blockNumber

        return if (hash != null && from != null && data != null && value != null && blockNumber != null) {
            FullContractDeploymentTransactionInfo(
                hash = hash,
                from = from,
                deployedContractAddress = contractAddress,
                data = data,
                value = value,
                binary = binary,
                blockNumber = blockNumber,
                events = emptyList()
            )
        } else {
            ContractBinaryInfo(
                deployedContractAddress = contractAddress,
                binary = binary
            )
        }
    }
}
