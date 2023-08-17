package polycode.features.contract.deployment.repository

import polycode.generated.jooq.id.ProjectId
import polycode.model.result.ContractMetadata
import polycode.util.ContractId
import polycode.util.InterfaceId

interface ContractMetadataRepository {
    fun createOrUpdate(contractMetadata: ContractMetadata): Boolean
    fun updateInterfaces(contractId: ContractId, projectId: ProjectId, interfaces: List<InterfaceId>): Boolean
    fun exists(contractId: ContractId, projectId: ProjectId): Boolean
}
