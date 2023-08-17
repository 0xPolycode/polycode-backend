package polycode.features.contract.functioncall.model.params

import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.util.ContractAddress

data class PreStoreContractFunctionCallRequestParams(
    val createParams: CreateContractFunctionCallRequestParams,
    val deployedContractId: ContractDeploymentRequestId?,
    val contractAddress: ContractAddress
)
