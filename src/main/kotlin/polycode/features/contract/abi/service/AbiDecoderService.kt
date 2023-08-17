package polycode.features.contract.abi.service

import polycode.features.contract.abi.model.AbiType

interface AbiDecoderService {
    fun decode(types: List<AbiType>, encodedInput: String): List<Any>
}
