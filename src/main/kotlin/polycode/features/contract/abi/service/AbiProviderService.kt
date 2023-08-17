package polycode.features.contract.abi.service

import polycode.blockchain.properties.ChainSpec
import polycode.features.contract.importing.model.json.DecompiledContractJson
import polycode.util.ContractAddress

interface AbiProviderService {
    fun getContractAbi(
        bytecode: String,
        deployedBytecode: String,
        contractAddress: ContractAddress,
        chainSpec: ChainSpec
    ): DecompiledContractJson?
}
