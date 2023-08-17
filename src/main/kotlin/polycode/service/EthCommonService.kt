package polycode.service

import polycode.features.api.access.model.result.Project
import polycode.model.DeserializableEvent
import polycode.model.params.ParamsFactory
import polycode.model.result.BlockchainTransactionInfo
import polycode.util.ChainId
import polycode.util.TransactionHash

interface EthCommonService {
    fun <P, R> createDatabaseParams(factory: ParamsFactory<P, R>, params: P, project: Project): R
    fun <R> fetchResource(resource: R?, message: String): R
    fun fetchTransactionInfo(
        txHash: TransactionHash?,
        chainId: ChainId,
        customRpcUrl: String?,
        events: List<DeserializableEvent>
    ): BlockchainTransactionInfo?
}
