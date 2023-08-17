package polycode.features.functions.decoding.model

import polycode.features.functions.encoding.model.FunctionArgument

data class EthFunction(val name: String, val arguments: List<FunctionArgument>?)
