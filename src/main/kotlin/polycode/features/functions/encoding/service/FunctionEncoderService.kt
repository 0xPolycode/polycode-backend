package polycode.features.functions.encoding.service

import polycode.features.functions.encoding.model.FunctionArgument
import polycode.util.FunctionData

interface FunctionEncoderService {
    fun encode(
        functionName: String,
        arguments: List<FunctionArgument>
    ): FunctionData

    fun encodeConstructor(arguments: List<FunctionArgument>): FunctionData
}
