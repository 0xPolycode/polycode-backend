package polycode.features.contract.importing.service

import polycode.features.contract.importing.model.json.DecompiledContractJson
import polycode.util.ContractBinaryData

interface ContractDecompilerService {
    fun decompile(contractBinary: ContractBinaryData): DecompiledContractJson
}
