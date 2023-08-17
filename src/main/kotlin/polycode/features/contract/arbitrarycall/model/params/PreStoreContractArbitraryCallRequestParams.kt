package polycode.features.contract.arbitrarycall.model.params

import com.fasterxml.jackson.databind.JsonNode
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.util.ContractAddress

data class PreStoreContractArbitraryCallRequestParams(
    val createParams: CreateContractArbitraryCallRequestParams,
    val deployedContractId: ContractDeploymentRequestId?,
    val functionName: String?,
    val functionParams: JsonNode?,
    val contractAddress: ContractAddress
)
