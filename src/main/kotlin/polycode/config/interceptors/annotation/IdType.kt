package polycode.config.interceptors.annotation

enum class IdType(val idVariableName: String) {
    PROJECT_ID("projectId"),
    ASSET_BALANCE_REQUEST_ID("id"),
    ASSET_MULTI_SEND_REQUEST_ID("id"),
    ASSET_SEND_REQUEST_ID("id"),
    AUTHORIZATION_REQUEST_ID("id"),
    CONTRACT_DEPLOYMENT_REQUEST_ID("id"),
    FUNCTION_CALL_REQUEST_ID("id"),
    ARBITRARY_CALL_REQUEST_ID("id"),
    ERC20_LOCK_REQUEST_ID("id"),
    ASSET_SNAPSHOT_ID("id")
}
