package polycode.features.api.usage.repository

import mu.KLogging
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.TableField
import org.springframework.stereotype.Repository
import polycode.config.interceptors.annotation.IdType
import polycode.generated.jooq.id.AssetBalanceRequestId
import polycode.generated.jooq.id.AssetMultiSendRequestId
import polycode.generated.jooq.id.AssetSendRequestId
import polycode.generated.jooq.id.AssetSnapshotId
import polycode.generated.jooq.id.AuthorizationRequestId
import polycode.generated.jooq.id.ContractArbitraryCallRequestId
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.generated.jooq.id.ContractFunctionCallRequestId
import polycode.generated.jooq.id.DatabaseId
import polycode.generated.jooq.id.Erc20LockRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.generated.jooq.id.UserId
import polycode.generated.jooq.tables.AssetBalanceRequestTable
import polycode.generated.jooq.tables.AssetMultiSendRequestTable
import polycode.generated.jooq.tables.AssetSendRequestTable
import polycode.generated.jooq.tables.AssetSnapshotTable
import polycode.generated.jooq.tables.AuthorizationRequestTable
import polycode.generated.jooq.tables.ContractArbitraryCallRequestTable
import polycode.generated.jooq.tables.ContractDeploymentRequestTable
import polycode.generated.jooq.tables.ContractFunctionCallRequestTable
import polycode.generated.jooq.tables.Erc20LockRequestTable
import polycode.generated.jooq.tables.ProjectTable
import java.util.UUID

@Repository
class JooqUserIdResolverRepository(private val dslContext: DSLContext) : UserIdResolverRepository {

    companion object : KLogging()

    override fun getByProjectId(projectId: ProjectId): UserId? = getUserId(IdType.PROJECT_ID, projectId.value)

    // we want compiler type-safety of exhaustive matching of enum elements, so there is no way to reduce complexity
    @Suppress("ComplexMethod")
    override fun getUserId(idType: IdType, id: UUID): UserId? {
        logger.debug { "Resolving project ID, idType: $idType, id: $id" }

        val projectId = when (idType) {
            IdType.PROJECT_ID ->
                ProjectTable.run { ID.select(ProjectId(id), ID) }

            IdType.ASSET_BALANCE_REQUEST_ID ->
                AssetBalanceRequestTable.run { ID.select(AssetBalanceRequestId(id), PROJECT_ID) }

            IdType.ASSET_MULTI_SEND_REQUEST_ID ->
                AssetMultiSendRequestTable.run { ID.select(AssetMultiSendRequestId(id), PROJECT_ID) }

            IdType.ASSET_SEND_REQUEST_ID ->
                AssetSendRequestTable.run { ID.select(AssetSendRequestId(id), PROJECT_ID) }

            IdType.AUTHORIZATION_REQUEST_ID ->
                AuthorizationRequestTable.run { ID.select(AuthorizationRequestId(id), PROJECT_ID) }

            IdType.CONTRACT_DEPLOYMENT_REQUEST_ID ->
                ContractDeploymentRequestTable.run { ID.select(ContractDeploymentRequestId(id), PROJECT_ID) }

            IdType.FUNCTION_CALL_REQUEST_ID ->
                ContractFunctionCallRequestTable.run { ID.select(ContractFunctionCallRequestId(id), PROJECT_ID) }

            IdType.ARBITRARY_CALL_REQUEST_ID ->
                ContractArbitraryCallRequestTable.run { ID.select(ContractArbitraryCallRequestId(id), PROJECT_ID) }

            IdType.ERC20_LOCK_REQUEST_ID ->
                Erc20LockRequestTable.run { ID.select(Erc20LockRequestId(id), PROJECT_ID) }

            IdType.ASSET_SNAPSHOT_ID ->
                AssetSnapshotTable.run { ID.select(AssetSnapshotId(id), PROJECT_ID) }
        }

        return projectId?.let {
            logger.debug { "Project user ID, projectId: $it" }
            dslContext.select(ProjectTable.OWNER_ID)
                .from(ProjectTable)
                .where(ProjectTable.ID.eq(it))
                .fetchOne(ProjectTable.OWNER_ID)
        }
    }

    private fun <R : Record, I : DatabaseId> TableField<R, I>.select(
        id: I,
        projectIdField: TableField<*, ProjectId>
    ): ProjectId? =
        dslContext.select(projectIdField)
            .from(table)
            .where(this.eq(id))
            .fetchOne(projectIdField)
}
