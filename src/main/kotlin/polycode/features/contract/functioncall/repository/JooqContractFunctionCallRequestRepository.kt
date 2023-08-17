package polycode.features.contract.functioncall.repository

import mu.KLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.impl.DSL.coalesce
import org.springframework.stereotype.Repository
import polycode.features.contract.functioncall.model.filters.ContractFunctionCallRequestFilters
import polycode.features.contract.functioncall.model.params.StoreContractFunctionCallRequestParams
import polycode.features.contract.functioncall.model.result.ContractFunctionCallRequest
import polycode.generated.jooq.id.ContractFunctionCallRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.tables.ContractFunctionCallRequestTable
import polycode.generated.jooq.tables.records.ContractFunctionCallRequestRecord
import polycode.model.ScreenConfig
import polycode.util.TransactionHash
import polycode.util.WalletAddress

@Repository
class JooqContractFunctionCallRequestRepository(
    private val dslContext: DSLContext
) : ContractFunctionCallRequestRepository {

    companion object : KLogging()

    override fun store(params: StoreContractFunctionCallRequestParams): ContractFunctionCallRequest {
        logger.info { "Store contract function call request, params: $params" }
        val record = ContractFunctionCallRequestRecord(
            id = params.id,
            deployedContractId = params.deployedContractId,
            contractAddress = params.contractAddress,
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

    override fun getById(id: ContractFunctionCallRequestId): ContractFunctionCallRequest? {
        logger.debug { "Get contract function call request by id: $id" }
        return dslContext.selectFrom(ContractFunctionCallRequestTable)
            .where(ContractFunctionCallRequestTable.ID.eq(id))
            .fetchOne { it.toModel() }
    }

    override fun getAllByProjectId(
        projectId: ProjectId,
        filters: ContractFunctionCallRequestFilters
    ): List<ContractFunctionCallRequest> {
        logger.debug { "Get contract function call requests by projectId: $projectId, filters: $filters" }

        val conditions = listOfNotNull(
            ContractFunctionCallRequestTable.PROJECT_ID.eq(projectId),
            filters.deployedContractId?.let { ContractFunctionCallRequestTable.DEPLOYED_CONTRACT_ID.eq(it) },
            filters.contractAddress?.let { ContractFunctionCallRequestTable.CONTRACT_ADDRESS.eq(it) },
        )

        return dslContext.selectFrom(ContractFunctionCallRequestTable)
            .where(conditions)
            .orderBy(ContractFunctionCallRequestTable.CREATED_AT.asc())
            .fetch { it.toModel() }
    }

    override fun setTxInfo(id: ContractFunctionCallRequestId, txHash: TransactionHash, caller: WalletAddress): Boolean {
        logger.info { "Set txInfo for contract function call request, id: $id, txHash: $txHash, caller: $caller" }
        return dslContext.update(ContractFunctionCallRequestTable)
            .set(ContractFunctionCallRequestTable.TX_HASH, txHash)
            .set(
                ContractFunctionCallRequestTable.CALLER_ADDRESS,
                coalesce(ContractFunctionCallRequestTable.CALLER_ADDRESS, caller)
            )
            .where(
                DSL.and(
                    ContractFunctionCallRequestTable.ID.eq(id),
                    ContractFunctionCallRequestTable.TX_HASH.isNull()
                )
            )
            .execute() > 0
    }

    private fun ContractFunctionCallRequestRecord.toModel() =
        ContractFunctionCallRequest(
            id = id,
            deployedContractId = deployedContractId,
            contractAddress = contractAddress,
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
