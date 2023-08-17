package polycode.features.contract.interfaces.service

import polycode.features.contract.deployment.model.result.ContractDecorator
import polycode.features.contract.interfaces.model.result.MatchingContractInterfaces
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.generated.jooq.id.ProjectId
import polycode.util.InterfaceId

interface ContractInterfacesService {
    fun attachMatchingInterfacesToDecorator(contractDecorator: ContractDecorator): ContractDecorator
    fun getSuggestedInterfacesForImportedSmartContract(id: ContractDeploymentRequestId): MatchingContractInterfaces
    fun addInterfacesToImportedContract(
        importedContractId: ContractDeploymentRequestId,
        projectId: ProjectId,
        interfaces: List<InterfaceId>
    )

    fun removeInterfacesFromImportedContract(
        importedContractId: ContractDeploymentRequestId,
        projectId: ProjectId,
        interfaces: List<InterfaceId>
    )

    fun setImportedContractInterfaces(
        importedContractId: ContractDeploymentRequestId,
        projectId: ProjectId,
        interfaces: List<InterfaceId>
    )
}
