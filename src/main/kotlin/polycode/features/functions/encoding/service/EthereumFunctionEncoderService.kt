package polycode.features.functions.encoding.service

import org.springframework.stereotype.Service
import org.web3j.abi.FunctionEncoder
import org.web3j.abi.datatypes.Function
import polycode.features.functions.encoding.model.FunctionArgument
import polycode.util.FunctionData

@Service
class EthereumFunctionEncoderService : FunctionEncoderService {
    override fun encode(
        functionName: String,
        arguments: List<FunctionArgument>
    ): FunctionData {
        val function = Function(
            functionName,
            arguments.map { it.value },
            emptyList()
        )
        val data = FunctionEncoder.encode(function)
        return FunctionData(data)
    }

    override fun encodeConstructor(arguments: List<FunctionArgument>): FunctionData {
        val data = FunctionEncoder.encodeConstructor(arguments.map { it.value })
        return FunctionData(data)
    }
}
