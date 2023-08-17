package polycode.features.contract.arbitrarycall.model.params

import com.fasterxml.jackson.databind.JsonNode
import polycode.features.api.access.model.result.Project
import polycode.generated.jooq.id.ContractArbitraryCallRequestId
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.model.ScreenConfig
import polycode.model.params.ParamsFactory
import polycode.util.Balance
import polycode.util.ChainId
import polycode.util.ContractAddress
import polycode.util.FunctionData
import polycode.util.UtcDateTime
import polycode.util.WalletAddress
import java.util.UUID
import polycode.features.contract.arbitrarycall.model.params.PreStoreContractArbitraryCallRequestParams as PreStoreParams

data class StoreContractArbitraryCallRequestParams(
    val id: ContractArbitraryCallRequestId,
    val deployedContractId: ContractDeploymentRequestId?,
    val contractAddress: ContractAddress,
    val functionData: FunctionData,
    val functionName: String?,
    val functionParams: JsonNode?,
    val ethAmount: Balance,
    val chainId: ChainId,
    val redirectUrl: String,
    val projectId: ProjectId,
    val createdAt: UtcDateTime,
    val arbitraryData: JsonNode?,
    val screenConfig: ScreenConfig,
    val callerAddress: WalletAddress?
) {
    companion object : ParamsFactory<PreStoreParams, StoreContractArbitraryCallRequestParams> {
        private const val PATH = "/request-arbitrary-call/\${id}/action"

        override fun fromCreateParams(
            id: UUID,
            params: PreStoreParams,
            project: Project,
            createdAt: UtcDateTime
        ) = StoreContractArbitraryCallRequestParams(
            id = ContractArbitraryCallRequestId(id),
            deployedContractId = params.deployedContractId,
            contractAddress = params.contractAddress,
            functionData = params.createParams.functionData,
            functionName = params.functionName,
            functionParams = params.functionParams,
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
