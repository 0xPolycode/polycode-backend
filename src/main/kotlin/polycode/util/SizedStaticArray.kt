package polycode.util

import org.web3j.abi.datatypes.StaticArray
import org.web3j.abi.datatypes.Type

class SizedStaticArray<T : Type<*>>(type: Class<T>, values: List<T>) : StaticArray<T>(type, values.size, values)
