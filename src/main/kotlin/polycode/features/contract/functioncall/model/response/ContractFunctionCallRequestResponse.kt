package polycode.features.contract.functioncall.model.response

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import polycode.features.contract.functioncall.model.result.ContractFunctionCallRequest
import polycode.features.functions.encoding.model.FunctionArgumentSchema
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.generated.jooq.id.ContractFunctionCallRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.model.ScreenConfig
import polycode.model.response.EventInfoResponse
import polycode.model.response.TransactionResponse
import polycode.util.Status
import polycode.util.WithFunctionData
import polycode.util.WithTransactionAndFunctionData
import polycode.util.annotation.SchemaIgnore
import polycode.util.annotation.SchemaName
import java.math.BigInteger
import java.time.OffsetDateTime

data class ContractFunctionCallRequestResponse(
    val id: ContractFunctionCallRequestId,
    val status: Status,
    val deployedContractId: ContractDeploymentRequestId?,
    val contractAddress: String,
    val functionName: String,
    @SchemaIgnore
    val functionParams: JsonNode,
    val functionCallData: String,
    val ethAmount: BigInteger,
    val chainId: Long,
    val redirectUrl: String,
    val projectId: ProjectId,
    val createdAt: OffsetDateTime,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig?,
    val callerAddress: String?,
    val functionCallTx: TransactionResponse,
    val events: List<EventInfoResponse>?
) {
    constructor(contractFunctionCallRequest: WithFunctionData<ContractFunctionCallRequest>) : this(
        id = contractFunctionCallRequest.value.id,
        status = Status.PENDING,
        deployedContractId = contractFunctionCallRequest.value.deployedContractId,
        contractAddress = contractFunctionCallRequest.value.contractAddress.rawValue,
        functionName = contractFunctionCallRequest.value.functionName,
        functionParams = contractFunctionCallRequest.value.functionParams,
        functionCallData = contractFunctionCallRequest.data.value,
        ethAmount = contractFunctionCallRequest.value.ethAmount.rawValue,
        chainId = contractFunctionCallRequest.value.chainId.value,
        redirectUrl = contractFunctionCallRequest.value.redirectUrl,
        projectId = contractFunctionCallRequest.value.projectId,
        createdAt = contractFunctionCallRequest.value.createdAt.value,
        arbitraryData = contractFunctionCallRequest.value.arbitraryData,
        screenConfig = contractFunctionCallRequest.value.screenConfig.orEmpty(),
        callerAddress = contractFunctionCallRequest.value.callerAddress?.rawValue,
        functionCallTx = TransactionResponse.unmined(
            from = contractFunctionCallRequest.value.callerAddress,
            to = contractFunctionCallRequest.value.contractAddress,
            data = contractFunctionCallRequest.data,
            value = contractFunctionCallRequest.value.ethAmount,
        ),
        events = null
    )

    constructor(contractFunctionCallRequest: WithTransactionAndFunctionData<ContractFunctionCallRequest>) : this(
        id = contractFunctionCallRequest.value.id,
        status = contractFunctionCallRequest.status,
        deployedContractId = contractFunctionCallRequest.value.deployedContractId,
        contractAddress = contractFunctionCallRequest.value.contractAddress.rawValue,
        functionName = contractFunctionCallRequest.value.functionName,
        functionParams = contractFunctionCallRequest.value.functionParams,
        functionCallData = contractFunctionCallRequest.functionData.value,
        ethAmount = contractFunctionCallRequest.value.ethAmount.rawValue,
        chainId = contractFunctionCallRequest.value.chainId.value,
        redirectUrl = contractFunctionCallRequest.value.redirectUrl,
        projectId = contractFunctionCallRequest.value.projectId,
        createdAt = contractFunctionCallRequest.value.createdAt.value,
        arbitraryData = contractFunctionCallRequest.value.arbitraryData,
        screenConfig = contractFunctionCallRequest.value.screenConfig.orEmpty(),
        callerAddress = contractFunctionCallRequest.value.callerAddress?.rawValue,
        functionCallTx = TransactionResponse(contractFunctionCallRequest.transactionData),
        events = contractFunctionCallRequest.transactionData.events?.map { EventInfoResponse(it) }
    )

    @Suppress("unused") // used for JSON schema generation
    @JsonIgnore
    @SchemaName("function_params")
    private val schemaFunctionParams: List<FunctionArgumentSchema> = emptyList()
}
