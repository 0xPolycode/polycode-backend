package polycode.features.contract.interfaces.repository

import polycode.features.contract.deployment.model.json.InterfaceManifestJson
import polycode.features.contract.deployment.model.json.InterfaceManifestJsonWithId
import polycode.features.contract.interfaces.model.filters.ContractInterfaceFilters
import polycode.util.InterfaceId

interface ContractInterfacesRepository {
    fun store(id: InterfaceId, interfaceManifestJson: InterfaceManifestJson): InterfaceManifestJson
    fun store(id: InterfaceId, infoMd: String): String
    fun delete(id: InterfaceId): Boolean
    fun getById(id: InterfaceId): InterfaceManifestJson?
    fun getInfoMarkdownById(id: InterfaceId): String?
    fun getAll(filters: ContractInterfaceFilters): List<InterfaceManifestJsonWithId>
    fun getAllInfoMarkdownFiles(filters: ContractInterfaceFilters): List<String>
    fun getAllWithPartiallyMatchingInterfaces(
        abiFunctionSignatures: Set<String>,
        abiEventSignatures: Set<String>
    ): List<InterfaceManifestJsonWithId>
}
