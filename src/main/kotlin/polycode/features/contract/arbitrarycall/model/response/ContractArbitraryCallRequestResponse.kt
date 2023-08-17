package polycode.features.contract.arbitrarycall.model.response

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import polycode.features.contract.arbitrarycall.model.result.ContractArbitraryCallRequest
import polycode.features.functions.encoding.model.FunctionArgumentSchema
import polycode.generated.jooq.id.ContractArbitraryCallRequestId
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.model.ScreenConfig
import polycode.model.response.EventInfoResponse
import polycode.model.response.TransactionResponse
import polycode.util.Status
import polycode.util.WithTransactionData
import polycode.util.annotation.SchemaIgnore
import polycode.util.annotation.SchemaName
import java.math.BigInteger
import java.time.OffsetDateTime

data class ContractArbitraryCallRequestResponse(
    val id: ContractArbitraryCallRequestId,
    val status: Status,
    val deployedContractId: ContractDeploymentRequestId?,
    val contractAddress: String,
    val functionName: String?,
    @SchemaIgnore
    val functionParams: JsonNode?,
    val functionCallData: String,
    val ethAmount: BigInteger,
    val chainId: Long,
    val redirectUrl: String,
    val projectId: ProjectId,
    val createdAt: OffsetDateTime,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig?,
    val callerAddress: String?,
    val arbitraryCallTx: TransactionResponse,
    val events: List<EventInfoResponse>?
) {
    constructor(contractArbitraryCallRequest: ContractArbitraryCallRequest) : this(
        id = contractArbitraryCallRequest.id,
        status = Status.PENDING,
        deployedContractId = contractArbitraryCallRequest.deployedContractId,
        contractAddress = contractArbitraryCallRequest.contractAddress.rawValue,
        functionName = contractArbitraryCallRequest.functionName,
        functionParams = contractArbitraryCallRequest.functionParams,
        functionCallData = contractArbitraryCallRequest.functionData.value,
        ethAmount = contractArbitraryCallRequest.ethAmount.rawValue,
        chainId = contractArbitraryCallRequest.chainId.value,
        redirectUrl = contractArbitraryCallRequest.redirectUrl,
        projectId = contractArbitraryCallRequest.projectId,
        createdAt = contractArbitraryCallRequest.createdAt.value,
        arbitraryData = contractArbitraryCallRequest.arbitraryData,
        screenConfig = contractArbitraryCallRequest.screenConfig.orEmpty(),
        callerAddress = contractArbitraryCallRequest.callerAddress?.rawValue,
        arbitraryCallTx = TransactionResponse.unmined(
            from = contractArbitraryCallRequest.callerAddress,
            to = contractArbitraryCallRequest.contractAddress,
            data = contractArbitraryCallRequest.functionData,
            value = contractArbitraryCallRequest.ethAmount,
        ),
        events = null
    )

    constructor(contractArbitraryCallRequest: WithTransactionData<ContractArbitraryCallRequest>) : this(
        id = contractArbitraryCallRequest.value.id,
        status = contractArbitraryCallRequest.status,
        deployedContractId = contractArbitraryCallRequest.value.deployedContractId,
        contractAddress = contractArbitraryCallRequest.value.contractAddress.rawValue,
        functionName = contractArbitraryCallRequest.value.functionName,
        functionParams = contractArbitraryCallRequest.value.functionParams,
        functionCallData = contractArbitraryCallRequest.value.functionData.value,
        ethAmount = contractArbitraryCallRequest.value.ethAmount.rawValue,
        chainId = contractArbitraryCallRequest.value.chainId.value,
        redirectUrl = contractArbitraryCallRequest.value.redirectUrl,
        projectId = contractArbitraryCallRequest.value.projectId,
        createdAt = contractArbitraryCallRequest.value.createdAt.value,
        arbitraryData = contractArbitraryCallRequest.value.arbitraryData,
        screenConfig = contractArbitraryCallRequest.value.screenConfig.orEmpty(),
        callerAddress = contractArbitraryCallRequest.value.callerAddress?.rawValue,
        arbitraryCallTx = TransactionResponse(contractArbitraryCallRequest.transactionData),
        events = contractArbitraryCallRequest.transactionData.events?.map { EventInfoResponse(it) }
    )

    @Suppress("unused") // used for JSON schema generation
    @JsonIgnore
    @SchemaName("function_params")
    private val schemaFunctionParams: List<FunctionArgumentSchema> = emptyList()
}
