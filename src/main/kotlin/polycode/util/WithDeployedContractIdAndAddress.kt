package polycode.util

import polycode.generated.jooq.id.ContractDeploymentRequestId

data class WithDeployedContractIdAndAddress<T>(
    val value: T,
    val deployedContractId: ContractDeploymentRequestId?,
    val contractAddress: ContractAddress
)
