package polycode.service

import mu.KLogging
import org.springframework.stereotype.Service
import polycode.blockchain.BlockchainService
import polycode.blockchain.properties.ChainSpec
import polycode.exception.ResourceNotFoundException
import polycode.features.api.access.model.result.Project
import polycode.model.DeserializableEvent
import polycode.model.params.ParamsFactory
import polycode.model.result.BlockchainTransactionInfo
import polycode.util.ChainId
import polycode.util.TransactionHash

@Service
class EthCommonServiceImpl(
    private val uuidProvider: UuidProvider,
    private val utcDateTimeProvider: UtcDateTimeProvider,
    private val blockchainService: BlockchainService
) : EthCommonService {

    companion object : KLogging()

    override fun <P, R> createDatabaseParams(factory: ParamsFactory<P, R>, params: P, project: Project): R {
        return factory.fromCreateParams(
            id = uuidProvider.getRawUuid(),
            params = params,
            project = project,
            createdAt = utcDateTimeProvider.getUtcDateTime()
        )
    }

    override fun <R> fetchResource(resource: R?, message: String): R {
        return resource ?: throw ResourceNotFoundException(message)
    }

    override fun fetchTransactionInfo(
        txHash: TransactionHash?,
        chainId: ChainId,
        customRpcUrl: String?,
        events: List<DeserializableEvent>
    ): BlockchainTransactionInfo? = txHash?.let {
        blockchainService.fetchTransactionInfo(
            chainSpec = ChainSpec(
                chainId = chainId,
                customRpcUrl = customRpcUrl
            ),
            txHash = txHash,
            events = events
        )
    }
}
