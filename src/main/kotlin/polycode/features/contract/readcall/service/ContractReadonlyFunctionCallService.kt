package polycode.features.contract.readcall.service

import polycode.features.api.access.model.result.Project
import polycode.features.contract.readcall.model.params.CreateReadonlyFunctionCallParams
import polycode.features.contract.readcall.model.result.ReadonlyFunctionCallResult
import polycode.util.WithDeployedContractIdAndAddress

interface ContractReadonlyFunctionCallService {
    fun callReadonlyContractFunction(
        params: CreateReadonlyFunctionCallParams,
        project: Project
    ): WithDeployedContractIdAndAddress<ReadonlyFunctionCallResult>
}
