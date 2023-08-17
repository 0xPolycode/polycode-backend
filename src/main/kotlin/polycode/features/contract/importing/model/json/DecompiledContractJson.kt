package polycode.features.contract.importing.model.json

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import polycode.features.contract.deployment.model.json.ArtifactJson
import polycode.features.contract.deployment.model.json.ManifestJson

@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy::class)
data class DecompiledContractJson(
    val manifest: ManifestJson,
    val artifact: ArtifactJson,
    val infoMarkdown: String?
)
