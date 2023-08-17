package polycode.features.contract.importing.model.response

import polycode.features.contract.deployment.model.json.ArtifactJson
import polycode.features.contract.deployment.model.json.ManifestJson
import polycode.features.contract.deployment.model.response.ContractDecoratorResponse
import polycode.features.contract.deployment.model.result.ContractDecorator

data class ImportPreviewResponse(
    val manifest: ManifestJson,
    val artifact: ArtifactJson,
    val decorator: ContractDecoratorResponse
) {
    constructor(decorator: ContractDecorator) : this(
        manifest = decorator.manifest,
        artifact = decorator.artifact,
        decorator = ContractDecoratorResponse(decorator)
    )
}
