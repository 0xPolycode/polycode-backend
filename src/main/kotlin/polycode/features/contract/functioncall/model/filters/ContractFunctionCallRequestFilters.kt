package polycode.features.contract.functioncall.model.filters

import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.util.ContractAddress

data class ContractFunctionCallRequestFilters(
    val deployedContractId: ContractDeploymentRequestId?,
    val contractAddress: ContractAddress?
)
