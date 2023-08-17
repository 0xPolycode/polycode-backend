package polycode.features.contract.importing.service

import org.springframework.stereotype.Service
import org.springframework.web.client.HttpClientErrorException.BadRequest
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import polycode.config.ContractManifestServiceProperties
import polycode.exception.CannotDecompileContractBinaryException
import polycode.exception.ContractDecompilationTemporarilyUnavailableException
import polycode.features.contract.importing.model.json.DecompiledContractJson
import polycode.util.ContractBinaryData

@Service
class ExternalContractDecompilerService(
    private val externalContractDecompilerServiceRestTemplate: RestTemplate,
    private val contractManifestServiceProperties: ContractManifestServiceProperties
) : ContractDecompilerService {

    companion object {
        private data class Request(val bytecode: String)
    }

    override fun decompile(contractBinary: ContractBinaryData): DecompiledContractJson =
        try {
            externalContractDecompilerServiceRestTemplate.postForEntity(
                contractManifestServiceProperties.decompileContractPath,
                Request(contractBinary.value),
                DecompiledContractJson::class.java
            ).body ?: throw ContractDecompilationTemporarilyUnavailableException()
        } catch (e: BadRequest) {
            throw CannotDecompileContractBinaryException()
        } catch (e: RestClientException) {
            throw ContractDecompilationTemporarilyUnavailableException()
        }
}
