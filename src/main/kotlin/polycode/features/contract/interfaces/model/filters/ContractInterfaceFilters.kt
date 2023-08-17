package polycode.features.contract.interfaces.model.filters

import polycode.model.filters.AndList
import polycode.model.filters.OrList
import polycode.util.ContractTag

data class ContractInterfaceFilters(
    val interfaceTags: OrList<AndList<ContractTag>>,
)
