package polycode.features.contract.importing.service

import polycode.blockchain.properties.ChainSpec
import polycode.features.api.access.model.result.Project
import polycode.features.contract.deployment.model.result.ContractDecorator
import polycode.features.contract.importing.model.params.ImportContractParams
import polycode.generated.jooq.id.ContractDeploymentRequestId
import polycode.util.ContractAddress

interface ContractImportService {
    fun importExistingContract(params: ImportContractParams, project: Project): ContractDeploymentRequestId?
    fun importContract(params: ImportContractParams, project: Project): ContractDeploymentRequestId
    fun previewImport(contractAddress: ContractAddress, chainSpec: ChainSpec): ContractDecorator
}
