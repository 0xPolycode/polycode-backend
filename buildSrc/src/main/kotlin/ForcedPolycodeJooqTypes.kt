import org.gradle.configurationcache.extensions.capitalized

object ForcedPolycodeJooqTypes {

    data class JooqType(
        val userType: String,
        val includeExpression: String,
        val includeTypes: String,
        val converter: String
    ) {
        constructor(userType: String, includeExpression: String, includeTypes: String) : this(
            userType, includeExpression, includeTypes, userType + "Converter"
        )
    }

    private val domainIdTypes = listOf(
        "ADDRESS_BOOK_ID",
        "API_KEY_ID",
        "API_USAGE_PERIOD_ID",
        "ASSET_BALANCE_REQUEST_ID",
        "ASSET_MULTI_SEND_REQUEST_ID",
        "ASSET_SEND_REQUEST_ID",
        "AUTHORIZATION_REQUEST_ID",
        "CONTRACT_ARBITRARY_CALL_REQUEST_ID",
        "CONTRACT_DEPLOYMENT_REQUEST_ID",
        "CONTRACT_DEPLOYMENT_TRANSACTION_CACHE_ID",
        "CONTRACT_FUNCTION_CALL_REQUEST_ID",
        "CONTRACT_METADATA_ID",
        "ERC20_LOCK_REQUEST_ID",
        "FETCH_ACCOUNT_BALANCE_CACHE_ID",
        "FETCH_ERC20_ACCOUNT_BALANCE_CACHE_ID",
        "FETCH_TRANSACTION_INFO_CACHE_ID",
        "IMPORTED_CONTRACT_DECORATOR_ID",
        "MULTI_PAYMENT_TEMPLATE_ID",
        "MULTI_PAYMENT_TEMPLATE_ITEM_ID",
        "PROJECT_ID",
        "USER_ID",
        "MERKLE_TREE_ROOT_ID",
        "MERKLE_TREE_LEAF_ID",
        "ASSET_SNAPSHOT_ID"
    )

    val types = listOf(
        JooqType(
            userType = "polycode.util.ChainId",
            includeExpression = "chain_id",
            includeTypes = "BIGINT"
        ),
        JooqType(
            userType = "polycode.util.ContractAddress",
            includeExpression = "token_address|.*_contract_address|contract_address",
            includeTypes = "VARCHAR"
        ),
        JooqType(
            userType = "polycode.util.WalletAddress",
            includeExpression = ".*_address",
            includeTypes = "VARCHAR"
        ),
        JooqType(
            userType = "polycode.util.Balance",
            includeExpression = ".*_amount",
            includeTypes = "NUMERIC"
        ),
        JooqType(
            userType = "polycode.util.BlockNumber",
            includeExpression = "block_number",
            includeTypes = "NUMERIC"
        ),
        JooqType(
            userType = "polycode.util.TransactionHash",
            includeExpression = ".*tx_hash",
            includeTypes = "VARCHAR"
        ),
        JooqType(
            userType = "polycode.features.contract.deployment.model.json.ManifestJson",
            converter = "polycode.util.ManifestJsonConverter",
            includeExpression = "manifest_json",
            includeTypes = "JSON"
        ),
        JooqType(
            userType = "polycode.features.contract.deployment.model.json.ArtifactJson",
            converter = "polycode.util.ArtifactJsonConverter",
            includeExpression = "artifact_json",
            includeTypes = "JSON"
        ),
        JooqType(
            userType = "polycode.util.SignedMessage",
            includeExpression = "signed_message",
            includeTypes = "VARCHAR"
        ),
        JooqType(
            userType = "polycode.util.DurationSeconds",
            includeExpression = ".*_duration_seconds",
            includeTypes = "NUMERIC"
        ),
        JooqType(
            userType = "com.fasterxml.jackson.databind.JsonNode",
            converter = "polycode.util.JsonNodeConverter",
            includeExpression = ".*",
            includeTypes = "JSON"
        ),
        JooqType(
            userType = "polycode.util.UtcDateTime",
            includeExpression = ".*",
            includeTypes = "TIMESTAMPTZ"
        ),
        JooqType(
            userType = "polycode.util.BaseUrl",
            includeExpression = "base_redirect_url",
            includeTypes = "VARCHAR"
        ),
        JooqType(
            userType = "polycode.util.ContractId",
            includeExpression = "contract_id",
            includeTypes = "VARCHAR"
        ),
        JooqType(
            userType = "polycode.util.ContractBinaryData",
            includeExpression = "contract_data",
            includeTypes = "BYTEA"
        ),
        JooqType(
            userType = "polycode.util.FunctionData",
            includeExpression = "tx_data|function_data",
            includeTypes = "BYTEA"
        ),
        JooqType(
            userType = "polycode.features.payout.util.HashFunction",
            converter = "polycode.util.HashFunctionConverter",
            includeExpression = ".*",
            includeTypes = "HASH_FUNCTION"
        ),
        JooqType(
            userType = "polycode.features.payout.util.AssetSnapshotStatus",
            converter = "polycode.util.AssetSnapshotStatusConverter",
            includeExpression = ".*",
            includeTypes = "ASSET_SNAPSHOT_STATUS"
        ),
        JooqType(
            userType = "polycode.features.payout.util.AssetSnapshotFailureCause",
            converter = "polycode.util.AssetSnapshotFailureCauseConverter",
            includeExpression = ".*",
            includeTypes = "ASSET_SNAPSHOT_FAILURE_CAUSE"
        ),
        JooqType(
            userType = "polycode.features.payout.util.MerkleHash",
            converter = "polycode.util.MerkleHashConverter",
            includeExpression = "merkle_hash",
            includeTypes = "VARCHAR"
        ),
        JooqType(
            userType = "polycode.features.payout.util.IpfsHash",
            converter = "polycode.util.IpfsHashConverter",
            includeExpression = ".*_ipfs_hash",
            includeTypes = "VARCHAR"
        )
    ) + domainIdTypes.map {
        val typeName = it.split("_").joinToString("") { it.toLowerCase().capitalized() }

        JooqType(
            userType = "${Configurations.Jooq.packageName}.id.$typeName",
            converter = "${Configurations.Jooq.packageName}.converters.${typeName}Converter",
            includeExpression = ".*",
            includeTypes = it
        )
    }
}
