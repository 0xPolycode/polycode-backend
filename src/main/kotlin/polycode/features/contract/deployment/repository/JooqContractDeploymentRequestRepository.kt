package polycode.features.contract.deployment.repository

import mu.KLogging
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.impl.DSL
import org.jooq.impl.DSL.coalesce
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Repository
import polycode.exception.AliasAlreadyInUseException
import polycode.features.contract.deployment.model.filters.ContractDeploymentRequestFilters
import polycode.features.contract.deployment.model.params.StoreContractDeploymentRequestParams
import polycode.features.contract.deployment.model.result.ContractDeploymentRequest
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.tables.ContractDeploymentRequestTable
import polycode.generated.jooq.tables.ContractMetadataTable
import polycode.generated.jooq.tables.records.ContractDeploymentRequestRecord
import polycode.model.ScreenConfig
import polycode.model.filters.AndList
import polycode.model.filters.OrList
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.ContractId
import polycode.util.ContractTag
import polycode.util.InterfaceId
import polycode.util.TransactionHash
import polycode.util.WalletAddress

@Suppress("TooManyFunctions")
@Repository
class JooqContractDeploymentRequestRepository(
    private val dslContext: DSLContext
) : ContractDeploymentRequestRepository {

    companion object : KLogging()

    override fun store(
        params: StoreContractDeploymentRequestParams,
        metadataProjectId: ProjectId
    ): ContractDeploymentRequest {
        logger.info { "Store contract deployment request, params: $params, metadataProjectId: $metadataProjectId" }
        val contractMetadataId = dslContext.select(ContractMetadataTable.ID)
            .from(ContractMetadataTable)
            .where(
                ContractMetadataTable.CONTRACT_ID.eq(params.contractId),
                ContractMetadataTable.PROJECT_ID.eq(metadataProjectId)
            )
            .fetchOne(ContractMetadataTable.ID)

        val record = ContractDeploymentRequestRecord(
            id = params.id,
            alias = params.alias,
            contractMetadataId = contractMetadataId!!,
            contractData = params.contractData,
            constructorParams = params.constructorParams,
            initialEthAmount = params.initialEthAmount,
            chainId = params.chainId,
            redirectUrl = params.redirectUrl,
            projectId = params.projectId,
            createdAt = params.createdAt,
            arbitraryData = params.arbitraryData,
            screenBeforeActionMessage = params.screenConfig.beforeActionMessage,
            screenAfterActionMessage = params.screenConfig.afterActionMessage,
            contractAddress = null,
            deployerAddress = params.deployerAddress,
            txHash = null,
            imported = params.imported,
            deleted = false,
            proxy = params.proxy,
            implementationContractAddress = params.implementationContractAddress
        )

        try {
            dslContext.executeInsert(record)
        } catch (e: DuplicateKeyException) {
            throw AliasAlreadyInUseException(params.alias)
        }

        return getById(params.id)!!
    }

    override fun markAsDeleted(id: ContractDeploymentRequestId): Boolean {
        logger.info { "Marking contract deployment request as deleted, id: $id" }
        return dslContext.update(ContractDeploymentRequestTable)
            .set(ContractDeploymentRequestTable.DELETED, true)
            .where(
                DSL.and(
                    ContractDeploymentRequestTable.ID.eq(id),
                    ContractDeploymentRequestTable.DELETED.eq(false)
                )
            )
            .execute() > 0
    }

    override fun getById(id: ContractDeploymentRequestId): ContractDeploymentRequest? {
        logger.debug { "Get contract deployment request by id: $id" }
        return dslContext.selectWithJoin()
            .where(
                DSL.and(
                    ContractDeploymentRequestTable.ID.eq(id),
                    ContractDeploymentRequestTable.DELETED.eq(false)
                )
            )
            .fetchOne { it.toModel() }
    }

    override fun getByAliasAndProjectId(alias: String, projectId: ProjectId): ContractDeploymentRequest? {
        logger.debug { "Get contract deployment request by alias: $alias, projectId: $projectId" }
        return dslContext.selectWithJoin()
            .where(
                DSL.and(
                    ContractDeploymentRequestTable.ALIAS.eq(alias),
                    ContractDeploymentRequestTable.PROJECT_ID.eq(projectId),
                    ContractDeploymentRequestTable.DELETED.eq(false)
                )
            )
            .fetchOne { it.toModel() }
    }

    override fun getByContractAddressAndChainId(
        contractAddress: ContractAddress,
        chainId: ChainId
    ): ContractDeploymentRequest? {
        logger.debug { "Get contract deployment request by contractAddress: $contractAddress, chainId: $chainId" }
        return dslContext.selectWithJoin()
            .where(
                DSL.and(
                    ContractDeploymentRequestTable.CONTRACT_ADDRESS.eq(contractAddress),
                    ContractDeploymentRequestTable.CHAIN_ID.eq(chainId),
                    ContractDeploymentRequestTable.DELETED.eq(false)
                )
            )
            .orderBy(ContractDeploymentRequestTable.CREATED_AT.asc())
            .limit(1)
            .fetchOne { it.toModel() }
    }

    override fun getByContractAddressChainIdAndProjectId(
        contractAddress: ContractAddress,
        chainId: ChainId,
        projectId: ProjectId
    ): ContractDeploymentRequest? {
        logger.debug {
            "Get contract deployment request by contractAddress: $contractAddress, chainId: $chainId," +
                " projectId: $projectId"
        }
        return dslContext.selectWithJoin()
            .where(
                DSL.and(
                    ContractDeploymentRequestTable.CONTRACT_ADDRESS.eq(contractAddress),
                    ContractDeploymentRequestTable.CHAIN_ID.eq(chainId),
                    ContractDeploymentRequestTable.PROJECT_ID.eq(projectId),
                    ContractDeploymentRequestTable.DELETED.eq(false)
                )
            )
            .orderBy(ContractDeploymentRequestTable.CREATED_AT.asc())
            .limit(1)
            .fetchOne { it.toModel() }
    }

    override fun getAllByProjectId(
        projectId: ProjectId,
        filters: ContractDeploymentRequestFilters
    ): List<ContractDeploymentRequest> {
        logger.debug { "Get contract deployment requests by projectId: $projectId, filters: $filters" }

        val conditions = listOfNotNull(
            ContractDeploymentRequestTable.PROJECT_ID.eq(projectId),
            ContractDeploymentRequestTable.DELETED.eq(false),
            filters.contractIds.orCondition(),
            filters.contractTags.orAndCondition { it.contractTagsAndCondition() },
            filters.contractImplements.orAndCondition { it.contractTraitsAndCondition() },
            filters.deployedOnly.takeIf { it }?.let {
                DSL.or(
                    ContractDeploymentRequestTable.TX_HASH.isNotNull(),
                    ContractDeploymentRequestTable.IMPORTED.isTrue()
                )
            }
        )

        return dslContext.selectWithJoin()
            .where(conditions)
            .orderBy(ContractDeploymentRequestTable.CREATED_AT.asc())
            .fetch { it.toModel() }
    }

    override fun setTxInfo(id: ContractDeploymentRequestId, txHash: TransactionHash, deployer: WalletAddress): Boolean {
        logger.info { "Set txInfo for contract deployment request, id: $id, txHash: $txHash, deployer: $deployer" }
        return dslContext.update(ContractDeploymentRequestTable)
            .set(ContractDeploymentRequestTable.TX_HASH, txHash)
            .set(
                ContractDeploymentRequestTable.DEPLOYER_ADDRESS,
                coalesce(ContractDeploymentRequestTable.DEPLOYER_ADDRESS, deployer)
            )
            .where(
                DSL.and(
                    ContractDeploymentRequestTable.ID.eq(id),
                    ContractDeploymentRequestTable.TX_HASH.isNull(),
                    ContractDeploymentRequestTable.DELETED.eq(false)
                )
            )
            .execute() > 0
    }

    override fun setContractAddress(id: ContractDeploymentRequestId, contractAddress: ContractAddress): Boolean {
        logger.info {
            "Set contract address for contract deployment request, id: $id, contractAddress: $contractAddress"
        }
        return dslContext.update(ContractDeploymentRequestTable)
            .set(ContractDeploymentRequestTable.CONTRACT_ADDRESS, contractAddress)
            .where(
                DSL.and(
                    ContractDeploymentRequestTable.ID.eq(id),
                    ContractDeploymentRequestTable.CONTRACT_ADDRESS.isNull(),
                    ContractDeploymentRequestTable.DELETED.eq(false)
                )
            )
            .execute() > 0
    }

    private fun Record.toModel(): ContractDeploymentRequest {
        val requestRecord = this.into(ContractDeploymentRequestTable)
        val metadataRecord = this.into(ContractMetadataTable)

        return ContractDeploymentRequest(
            id = requestRecord.id,
            alias = requestRecord.alias,
            name = metadataRecord.name,
            description = metadataRecord.description,
            contractId = metadataRecord.contractId,
            contractData = requestRecord.contractData,
            constructorParams = requestRecord.constructorParams,
            contractTags = metadataRecord.contractTags.map { ContractTag(it) },
            contractImplements = metadataRecord.contractImplements.map { InterfaceId(it) },
            initialEthAmount = requestRecord.initialEthAmount,
            chainId = requestRecord.chainId,
            redirectUrl = requestRecord.redirectUrl,
            projectId = requestRecord.projectId,
            createdAt = requestRecord.createdAt,
            arbitraryData = requestRecord.arbitraryData,
            screenConfig = ScreenConfig(
                beforeActionMessage = requestRecord.screenBeforeActionMessage,
                afterActionMessage = requestRecord.screenAfterActionMessage
            ),
            contractAddress = requestRecord.contractAddress,
            deployerAddress = requestRecord.deployerAddress,
            txHash = requestRecord.txHash,
            imported = requestRecord.imported,
            proxy = requestRecord.proxy,
            implementationContractAddress = requestRecord.implementationContractAddress
        )
    }

    private fun DSLContext.selectWithJoin() = select()
        .from(ContractDeploymentRequestTable)
        .join(ContractMetadataTable)
        .on(ContractDeploymentRequestTable.CONTRACT_METADATA_ID.eq(ContractMetadataTable.ID))

    private fun OrList<ContractId>.orCondition(): Condition? =
        takeIf { list.isNotEmpty() }?.let { ContractMetadataTable.CONTRACT_ID.`in`(it.list) }

    private fun AndList<ContractTag>.contractTagsAndCondition(): Condition? =
        takeIf { list.isNotEmpty() }?.let {
            ContractMetadataTable.CONTRACT_TAGS.contains(
                it.list.map { v -> v.value }.toTypedArray()
            )
        }

    private fun AndList<InterfaceId>.contractTraitsAndCondition(): Condition? =
        takeIf { list.isNotEmpty() }?.let {
            ContractMetadataTable.CONTRACT_IMPLEMENTS.contains(
                it.list.map { v -> v.value }.toTypedArray()
            )
        }

    private fun <T> OrList<AndList<T>>.orAndCondition(innerConditionMapping: (AndList<T>) -> Condition?): Condition? =
        list.mapNotNull(innerConditionMapping).takeIf { it.isNotEmpty() }?.let { DSL.or(it) }
}
