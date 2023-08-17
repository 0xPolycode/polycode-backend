package polycode.features.contract.interfaces.model.response

import polycode.features.contract.interfaces.model.result.MatchingContractInterfaces

data class SuggestedContractInterfaceManifestsResponse(
    val manifests: List<ContractInterfaceManifestResponse>,
    val bestMatchingInterfaces: List<String>
) {
    constructor(matchingInterfaces: MatchingContractInterfaces) : this(
        manifests = matchingInterfaces.manifests.map(::ContractInterfaceManifestResponse),
        bestMatchingInterfaces = matchingInterfaces.bestMatchingInterfaces.map { it.value }
    )
}
