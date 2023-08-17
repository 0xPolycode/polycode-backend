package polycode.features.contract.interfaces.model.response

import polycode.features.contract.deployment.model.json.EventDecorator
import polycode.features.contract.deployment.model.json.FunctionDecorator
import polycode.features.contract.deployment.model.json.InterfaceManifestJson
import polycode.features.contract.deployment.model.json.InterfaceManifestJsonWithId
import polycode.util.InterfaceId

data class ContractInterfaceManifestResponse(
    val id: String,
    val name: String?,
    val tags: List<String>,
    val description: String?,
    val eventDecorators: List<EventDecorator>,
    val functionDecorators: List<FunctionDecorator>
) {
    constructor(manifest: InterfaceManifestJsonWithId) : this(
        id = manifest.id.value,
        name = manifest.name,
        description = manifest.description,
        tags = manifest.tags.toList(),
        eventDecorators = manifest.matchingEventDecorators,
        functionDecorators = manifest.matchingFunctionDecorators
    )

    constructor(id: InterfaceId, manifest: InterfaceManifestJson) : this(
        id = id.value,
        name = manifest.name,
        description = manifest.description,
        tags = manifest.tags.toList(),
        eventDecorators = manifest.eventDecorators,
        functionDecorators = manifest.functionDecorators
    )
}
