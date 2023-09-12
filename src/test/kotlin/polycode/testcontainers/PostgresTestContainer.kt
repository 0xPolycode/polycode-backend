package polycode.testcontainers

import org.jooq.DSLContext
import org.testcontainers.containers.PostgreSQLContainer
import polycode.generated.jooq.tables.AddressBookTable
import polycode.generated.jooq.tables.ApiKeyTable
import polycode.generated.jooq.tables.ApiReadCallTable
import polycode.generated.jooq.tables.ApiUsagePeriodTable
import polycode.generated.jooq.tables.ApiWriteCallTable
import polycode.generated.jooq.tables.AssetBalanceRequestTable
import polycode.generated.jooq.tables.AssetMultiSendRequestTable
import polycode.generated.jooq.tables.AssetSendRequestTable
import polycode.generated.jooq.tables.AssetSnapshotTable
import polycode.generated.jooq.tables.AuthorizationRequestTable
import polycode.generated.jooq.tables.BlacklistedAddressTable
import polycode.generated.jooq.tables.ContractArbitraryCallRequestTable
import polycode.generated.jooq.tables.ContractDeploymentRequestTable
import polycode.generated.jooq.tables.ContractDeploymentTransactionCacheTable
import polycode.generated.jooq.tables.ContractFunctionCallRequestTable
import polycode.generated.jooq.tables.ContractMetadataTable
import polycode.generated.jooq.tables.Erc20LockRequestTable
import polycode.generated.jooq.tables.FetchAccountBalanceCacheTable
import polycode.generated.jooq.tables.FetchErc20AccountBalanceCacheTable
import polycode.generated.jooq.tables.FetchTransactionInfoCacheTable
import polycode.generated.jooq.tables.ImportedContractDecoratorTable
import polycode.generated.jooq.tables.MerkleTreeLeafNodeTable
import polycode.generated.jooq.tables.MerkleTreeRootTable
import polycode.generated.jooq.tables.MultiPaymentTemplateItemTable
import polycode.generated.jooq.tables.MultiPaymentTemplateTable
import polycode.generated.jooq.tables.ProjectTable
import polycode.generated.jooq.tables.UserIdentifierTable
import polycode.generated.jooq.tables.WalletLoginRequestTable

class PostgresTestContainer : PostgreSQLContainer<PostgresTestContainer>("postgres:13.4-alpine") {

    init {
        start()
        System.setProperty("POSTGRES_PORT", getMappedPort(POSTGRESQL_PORT).toString())
    }

    fun cleanAllDatabaseTables(dslContext: DSLContext) {
        dslContext.apply {
            deleteFrom(AddressBookTable).execute()
            deleteFrom(AuthorizationRequestTable).execute()
            deleteFrom(AssetBalanceRequestTable).execute()
            deleteFrom(AssetSendRequestTable).execute()
            deleteFrom(AssetMultiSendRequestTable).execute()
            deleteFrom(ContractFunctionCallRequestTable).execute()
            deleteFrom(ContractArbitraryCallRequestTable).execute()
            deleteFrom(ContractDeploymentRequestTable).execute()
            deleteFrom(ContractMetadataTable).execute()
            deleteFrom(Erc20LockRequestTable).execute()
            deleteFrom(MultiPaymentTemplateItemTable).execute()
            deleteFrom(MultiPaymentTemplateTable).execute()
            deleteFrom(ImportedContractDecoratorTable).execute()
            deleteFrom(ApiUsagePeriodTable).execute()
            deleteFrom(ApiWriteCallTable).execute()
            deleteFrom(ApiReadCallTable).execute()
            deleteFrom(FetchAccountBalanceCacheTable).execute()
            deleteFrom(FetchErc20AccountBalanceCacheTable).execute()
            deleteFrom(FetchTransactionInfoCacheTable).execute()
            deleteFrom(ContractDeploymentTransactionCacheTable).execute()
            deleteFrom(AssetSnapshotTable).execute()
            deleteFrom(MerkleTreeLeafNodeTable).execute()
            deleteFrom(MerkleTreeRootTable).execute()
            deleteFrom(ApiKeyTable).execute()
            deleteFrom(ProjectTable).execute()
            deleteFrom(UserIdentifierTable).execute()
            deleteFrom(BlacklistedAddressTable).execute()
            deleteFrom(WalletLoginRequestTable).execute()
        }
    }
}
