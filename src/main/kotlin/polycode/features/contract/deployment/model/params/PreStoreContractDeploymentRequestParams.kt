package polycode.features.contract.deployment.model.params

import polycode.features.contract.deployment.model.result.ContractDecorator
import polycode.util.FunctionData

data class PreStoreContractDeploymentRequestParams(
    val createParams: CreateContractDeploymentRequestParams,
    val contractDecorator: ContractDecorator,
    val encodedConstructor: FunctionData
)
