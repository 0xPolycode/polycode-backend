package polycode.features.contract.deployment.service

import polycode.features.api.access.model.result.Project
import polycode.features.contract.deployment.model.params.DeployedContractIdentifier
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.util.ContractAddress

interface DeployedContractIdentifierResolverService {
    fun resolveContractIdAndAddress(
        identifier: DeployedContractIdentifier,
        project: Project
    ): Pair<ContractDeploymentRequestId?, ContractAddress>
}
