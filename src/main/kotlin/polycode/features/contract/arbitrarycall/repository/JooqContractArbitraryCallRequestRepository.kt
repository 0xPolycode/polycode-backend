package polycode.features.contract.arbitrarycall.repository

import mu.KLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.impl.DSL.coalesce
import org.springframework.stereotype.Repository
import polycode.features.contract.arbitrarycall.model.filters.ContractArbitraryCallRequestFilters
import polycode.features.contract.arbitrarycall.model.params.StoreContractArbitraryCallRequestParams
import polycode.features.contract.arbitrarycall.model.result.ContractArbitraryCallRequest
import polycode.generated.jooq.id.ContractArbitraryCallRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.tables.ContractArbitraryCallRequestTable
import polycode.generated.jooq.tables.records.ContractArbitraryCallRequestRecord
import polycode.model.ScreenConfig
import polycode.util.TransactionHash
import polycode.util.WalletAddress

@Repository
class JooqContractArbitraryCallRequestRepository(
    private val dslContext: DSLContext
) : ContractArbitraryCallRequestRepository {

    companion object : KLogging()

    override fun store(params: StoreContractArbitraryCallRequestParams): ContractArbitraryCallRequest {
        logger.info { "Store contract arbitrary call request, params: $params" }
        val record = ContractArbitraryCallRequestRecord(
            id = params.id,
            deployedContractId = params.deployedContractId,
            contractAddress = params.contractAddress,
            functionData = params.functionData,
            functionName = params.functionName,
            functionParams = params.functionParams,
            ethAmount = params.ethAmount,
            chainId = params.chainId,
            redirectUrl = params.redirectUrl,
            projectId = params.projectId,
            createdAt = params.createdAt,
            arbitraryData = params.arbitraryData,
            screenBeforeActionMessage = params.screenConfig.beforeActionMessage,
            screenAfterActionMessage = params.screenConfig.afterActionMessage,
            callerAddress = params.callerAddress,
            txHash = null
        )
        dslContext.executeInsert(record)
        return record.toModel()
    }

    override fun getById(id: ContractArbitraryCallRequestId): ContractArbitraryCallRequest? {
        logger.debug { "Get contract arbitrary call request by id: $id" }
        return dslContext.selectFrom(ContractArbitraryCallRequestTable)
            .where(ContractArbitraryCallRequestTable.ID.eq(id))
            .fetchOne { it.toModel() }
    }

    override fun getAllByProjectId(
        projectId: ProjectId,
        filters: ContractArbitraryCallRequestFilters
    ): List<ContractArbitraryCallRequest> {
        logger.debug { "Get contract arbitrary call requests by projectId: $projectId, filters: $filters" }

        val conditions = listOfNotNull(
            ContractArbitraryCallRequestTable.PROJECT_ID.eq(projectId),
            filters.deployedContractId?.let { ContractArbitraryCallRequestTable.DEPLOYED_CONTRACT_ID.eq(it) },
            filters.contractAddress?.let { ContractArbitraryCallRequestTable.CONTRACT_ADDRESS.eq(it) },
        )

        return dslContext.selectFrom(ContractArbitraryCallRequestTable)
            .where(conditions)
            .orderBy(ContractArbitraryCallRequestTable.CREATED_AT.asc())
            .fetch { it.toModel() }
    }

    override fun setTxInfo(
        id: ContractArbitraryCallRequestId,
        txHash: TransactionHash,
        caller: WalletAddress
    ): Boolean {
        logger.info { "Set txInfo for contract arbitrary call request, id: $id, txHash: $txHash, caller: $caller" }
        return dslContext.update(ContractArbitraryCallRequestTable)
            .set(ContractArbitraryCallRequestTable.TX_HASH, txHash)
            .set(
                ContractArbitraryCallRequestTable.CALLER_ADDRESS,
                coalesce(ContractArbitraryCallRequestTable.CALLER_ADDRESS, caller)
            )
            .where(
                DSL.and(
                    ContractArbitraryCallRequestTable.ID.eq(id),
                    ContractArbitraryCallRequestTable.TX_HASH.isNull()
                )
            )
            .execute() > 0
    }

    private fun ContractArbitraryCallRequestRecord.toModel() =
        ContractArbitraryCallRequest(
            id = id,
            deployedContractId = deployedContractId,
            contractAddress = contractAddress,
            functionData = functionData,
            functionName = functionName,
            functionParams = functionParams,
            ethAmount = ethAmount,
            chainId = chainId,
            redirectUrl = redirectUrl,
            projectId = projectId,
            createdAt = createdAt,
            arbitraryData = arbitraryData,
            screenConfig = ScreenConfig(
                beforeActionMessage = screenBeforeActionMessage,
                afterActionMessage = screenAfterActionMessage
            ),
            callerAddress = callerAddress,
            txHash = txHash,
        )
}
