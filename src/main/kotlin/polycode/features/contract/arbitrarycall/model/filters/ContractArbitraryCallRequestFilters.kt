package polycode.features.contract.arbitrarycall.model.filters

import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.util.ContractAddress

data class ContractArbitraryCallRequestFilters(
    val deployedContractId: ContractDeploymentRequestId?,
    val contractAddress: ContractAddress?
)
