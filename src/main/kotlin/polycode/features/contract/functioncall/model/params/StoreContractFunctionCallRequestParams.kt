package polycode.features.contract.functioncall.model.params

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import polycode.features.api.access.model.result.Project
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.generated.jooq.id.ContractFunctionCallRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.model.ScreenConfig
import polycode.model.params.ParamsFactory
import polycode.util.Balance
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.UtcDateTime
import polycode.util.WalletAddress
import java.util.UUID
import polycode.features.contract.functioncall.model.params.PreStoreContractFunctionCallRequestParams as PreStoreParams

data class StoreContractFunctionCallRequestParams(
    val id: ContractFunctionCallRequestId,
    val deployedContractId: ContractDeploymentRequestId?,
    val contractAddress: ContractAddress,
    val functionName: String,
    val functionParams: JsonNode,
    val ethAmount: Balance,
    val chainId: ChainId,
    val redirectUrl: String,
    val projectId: ProjectId,
    val createdAt: UtcDateTime,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig,
    val callerAddress: WalletAddress?
) {
    companion object : ParamsFactory<PreStoreParams, StoreContractFunctionCallRequestParams> {
        private const val PATH = "/request-function-call/\${id}/action"
        private val objectMapper = ObjectMapper()

        override fun fromCreateParams(
            id: UUID,
            params: PreStoreParams,
            project: Project,
            createdAt: UtcDateTime
        ) = StoreContractFunctionCallRequestParams(
            id = ContractFunctionCallRequestId(id),
            deployedContractId = params.deployedContractId,
            contractAddress = params.contractAddress,
            functionName = params.createParams.functionName,
            functionParams = objectMapper.createArrayNode().addAll(
                params.createParams.functionParams.mapNotNull { it.rawJson }
            ),
            ethAmount = params.createParams.ethAmount,
            chainId = project.chainId,
            redirectUrl = project.createRedirectUrl(params.createParams.redirectUrl, id, PATH),
            projectId = project.id,
            createdAt = createdAt,
            arbitraryData = params.createParams.arbitraryData,
            screenConfig = params.createParams.screenConfig,
            callerAddress = params.createParams.callerAddress
        )
    }
}
