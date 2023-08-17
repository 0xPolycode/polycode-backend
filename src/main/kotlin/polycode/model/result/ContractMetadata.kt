package polycode.model.result

import polycode.generated.jooq.id.ContractMetadataId
import polycode.generated.jooq.id.ProjectId
import polycode.util.ContractId
import polycode.util.ContractTag
import polycode.util.InterfaceId

data class ContractMetadata(
    val id: ContractMetadataId,
    val name: String?,
    val description: String?,
    val contractId: ContractId,
    val contractTags: List<ContractTag>,
    val contractImplements: List<InterfaceId>,
    val projectId: ProjectId
)
