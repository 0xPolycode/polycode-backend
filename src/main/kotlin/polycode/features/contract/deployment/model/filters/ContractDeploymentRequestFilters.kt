package polycode.features.contract.deployment.model.filters

import polycode.model.filters.AndList
import polycode.model.filters.OrList
import polycode.util.ContractId
import polycode.util.ContractTag
import polycode.util.InterfaceId

data class ContractDeploymentRequestFilters(
    val contractIds: OrList<ContractId>,
    val contractTags: OrList<AndList<ContractTag>>,
    val contractImplements: OrList<AndList<InterfaceId>>,
    val deployedOnly: Boolean
)
