package polycode.features.contract.deployment.model.response

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.databind.JsonNode
import polycode.features.contract.deployment.model.result.ContractDeploymentRequest
import polycode.features.functions.encoding.model.FunctionArgumentSchema
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.model.ScreenConfig
import polycode.model.response.EventInfoResponse
import polycode.model.response.TransactionResponse
import polycode.util.FunctionData
import polycode.util.Status
import polycode.util.WithTransactionData
import polycode.util.ZeroAddress
import polycode.util.annotation.SchemaIgnore
import polycode.util.annotation.SchemaName
import java.math.BigInteger
import java.time.OffsetDateTime

data class ContractDeploymentRequestResponse(
    val id: ContractDeploymentRequestId,
    val alias: String,
    val name: String?,
    val description: String?,
    val status: Status,
    val contractId: String,
    val contractDeploymentData: String,
    @SchemaIgnore
    val constructorParams: JsonNode,
    val contractTags: List<String>,
    val contractImplements: List<String>,
    val initialEthAmount: BigInteger,
    val chainId: Long,
    val redirectUrl: String,
    val projectId: ProjectId,
    val createdAt: OffsetDateTime,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig?,
    val contractAddress: String?,
    val deployerAddress: String?,
    val deployTx: TransactionResponse,
    val imported: Boolean,
    val proxy: Boolean,
    val implementationContractAddress: String?,
    val events: List<EventInfoResponse>?
) {
    constructor(contractDeploymentRequest: ContractDeploymentRequest) : this(
        id = contractDeploymentRequest.id,
        alias = contractDeploymentRequest.alias,
        name = contractDeploymentRequest.name,
        description = contractDeploymentRequest.description,
        status = Status.PENDING,
        contractId = contractDeploymentRequest.contractId.value,
        contractDeploymentData = contractDeploymentRequest.contractData.withPrefix,
        constructorParams = contractDeploymentRequest.constructorParams,
        contractTags = contractDeploymentRequest.contractTags.map { it.value },
        contractImplements = contractDeploymentRequest.contractImplements.map { it.value },
        initialEthAmount = contractDeploymentRequest.initialEthAmount.rawValue,
        chainId = contractDeploymentRequest.chainId.value,
        redirectUrl = contractDeploymentRequest.redirectUrl,
        projectId = contractDeploymentRequest.projectId,
        createdAt = contractDeploymentRequest.createdAt.value,
        arbitraryData = contractDeploymentRequest.arbitraryData,
        screenConfig = contractDeploymentRequest.screenConfig.orEmpty(),
        contractAddress = contractDeploymentRequest.contractAddress?.rawValue,
        deployerAddress = contractDeploymentRequest.deployerAddress?.rawValue,
        deployTx = TransactionResponse.unmined(
            from = contractDeploymentRequest.deployerAddress,
            to = ZeroAddress,
            data = FunctionData(contractDeploymentRequest.contractData.value),
            value = contractDeploymentRequest.initialEthAmount
        ),
        imported = contractDeploymentRequest.imported,
        proxy = contractDeploymentRequest.proxy,
        implementationContractAddress = contractDeploymentRequest.implementationContractAddress?.rawValue,
        events = null
    )

    constructor(contractDeploymentRequest: WithTransactionData<ContractDeploymentRequest>) : this(
        id = contractDeploymentRequest.value.id,
        alias = contractDeploymentRequest.value.alias,
        name = contractDeploymentRequest.value.name,
        description = contractDeploymentRequest.value.description,
        status = contractDeploymentRequest.status,
        contractId = contractDeploymentRequest.value.contractId.value,
        contractDeploymentData = contractDeploymentRequest.value.contractData.withPrefix,
        constructorParams = contractDeploymentRequest.value.constructorParams,
        contractTags = contractDeploymentRequest.value.contractTags.map { it.value },
        contractImplements = contractDeploymentRequest.value.contractImplements.map { it.value },
        initialEthAmount = contractDeploymentRequest.value.initialEthAmount.rawValue,
        chainId = contractDeploymentRequest.value.chainId.value,
        redirectUrl = contractDeploymentRequest.value.redirectUrl,
        projectId = contractDeploymentRequest.value.projectId,
        createdAt = contractDeploymentRequest.value.createdAt.value,
        arbitraryData = contractDeploymentRequest.value.arbitraryData,
        screenConfig = contractDeploymentRequest.value.screenConfig.orEmpty(),
        contractAddress = contractDeploymentRequest.value.contractAddress?.rawValue,
        deployerAddress = contractDeploymentRequest.value.deployerAddress?.rawValue,
        deployTx = TransactionResponse(contractDeploymentRequest.transactionData),
        imported = contractDeploymentRequest.value.imported,
        proxy = contractDeploymentRequest.value.proxy,
        implementationContractAddress = contractDeploymentRequest.value.implementationContractAddress?.rawValue,
        events = contractDeploymentRequest.transactionData.events?.map { EventInfoResponse(it) }
    )

    @Suppress("unused") // used for JSON schema generation
    @JsonIgnore
    @SchemaName("constructor_params")
    private val schemaConstructorParams: List<FunctionArgumentSchema> = emptyList()
}
