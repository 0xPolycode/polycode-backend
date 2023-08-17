package polycode.exception

import polycode.util.annotation.Description

enum class ErrorCode {

    @Description("Indicates that the requested resource cannot be found")
    RESOURCE_NOT_FOUND,

    @Description("The chain ID specified in the project is not directly supported and requires a custom RPC URL")
    UNSUPPORTED_CHAIN_ID,

    @Description("Data cannot be temporarily read from blockchain, the same request should be retried at a later time")
    TEMPORARY_BLOCKCHAIN_READ_ERROR,

    @Description(
        "Requested data cannot be retrieved from the blockchain because it is not well formed (i.e. non-existent" +
            " contract address, calling non-existent contract function etc.)"
    )
    BLOCKCHAIN_READ_ERROR,

    @Description(
        "Requested event data cannot be retrieved from the blockchain because it is not well formed (i.e." +
            " non-existent contract address, calling non-existent contract function etc.)"
    )
    BLOCKCHAIN_EVENT_READ_ERROR,

    @Description("The transaction info has already been set for the requested resource and it cannot be overridden")
    TX_INFO_ALREADY_SET,

    @Description("The signed message has already been set for the requested resource and it cannot be overridden")
    SIGNED_MESSAGE_ALREADY_SET,

    @Description("Access to the provided resource is not allowed for the current user")
    ACCESS_FORBIDDEN,

    @Description("Provided authentication token has invalid format")
    BAD_AUTHENTICATION,

    @Description("The project already has an API key generated and no more keys can be generated for that project")
    API_KEY_ALREADY_EXISTS,

    @Description("The provided API key does not exist and is therefore invalid")
    NON_EXISTENT_API_KEY,

    @Description("API rate limit has been exceeded for the project")
    API_RATE_LIMIT_EXCEEDED,

    @Description("The request body is missing `token_address` value")
    MISSING_TOKEN_ADDRESS,

    @Description("The request body must not have non-null `token_address` value")
    TOKEN_ADDRESS_NOT_ALLOWED,

    @Description(
        "The provided alias is already in use for the specified resource type for the project associated with" +
            " provided API key"
    )
    ALIAS_ALREADY_IN_USE,

    @Description(
        "The contract has not yet been deployed on the blockchain, and therefore has no contract address defined -" +
            " the request should be retried at a later time"
    )
    CONTRACT_NOT_DEPLOYED,

    @Description("Indicates that one or more fields in the request body has an invalid value")
    INVALID_REQUEST_BODY,

    @Description("Indicates that one or more query parameters have invalid value")
    INVALID_QUERY_PARAM,

    @Description("No smart contract can be found for given contract address")
    CONTRACT_NOT_FOUND,

    @Description("Indicates that the imported contract binary does not match binary of requested contract ID")
    CONTRACT_BINARY_MISMATCH,

    @Description("Binary of the requested contract cannot be successfully decompiled")
    CANNOT_DECOMPILE_CONTRACT_BINARY,

    @Description("Decompilation of contract binary is currently unavailable and should be tried at a later time")
    CONTRACT_DECOMPILATION_TEMPORARILY_UNAVAILABLE,

    @Description("Contract decorator is incompatible with contract ABI specification")
    CONTRACT_DECORATOR_INCOMPATIBLE,

    @Description("Indicates that the requested contract interface cannot be found")
    CONTRACT_INTERFACE_NOT_FOUND,

    @Description("Indicates that file upload to IPFS has failed")
    IPFS_UPLOAD_FAILED,

    @Description(
        "Indicates that error occurred while decoding ABI-encoded response. This is most likely caused by" +
            " incompatible ABI types (e.g. decoding an int as a string)"
    )
    ABI_DECODING_FAILED
}
