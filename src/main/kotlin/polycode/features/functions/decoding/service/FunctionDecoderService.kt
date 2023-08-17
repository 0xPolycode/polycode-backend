package polycode.features.functions.decoding.service

import polycode.features.functions.decoding.model.EthFunction
import polycode.util.FunctionData

interface FunctionDecoderService {
    fun decode(data: FunctionData): EthFunction?
}
