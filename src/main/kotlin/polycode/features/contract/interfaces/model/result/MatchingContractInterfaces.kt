package polycode.features.contract.interfaces.model.result

import polycode.features.contract.deployment.model.json.InterfaceManifestJsonWithId
import polycode.util.InterfaceId

data class MatchingContractInterfaces(
    val manifests: List<InterfaceManifestJsonWithId>,
    val bestMatchingInterfaces: List<InterfaceId>
)
