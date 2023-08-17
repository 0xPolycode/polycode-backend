package polycode.features.contract.abi.model

sealed interface AbiType {
    fun isDynamic(): Boolean
    fun isIndexHashed(): Boolean
    fun valueSize(): Int
}

object UintType : AbiType {
    override fun isDynamic() = false
    override fun isIndexHashed() = false
    override fun valueSize(): Int = 1
}

object IntType : AbiType {
    override fun isDynamic() = false
    override fun isIndexHashed() = false
    override fun valueSize(): Int = 1
}

object AddressType : AbiType {
    override fun isDynamic() = false
    override fun isIndexHashed() = false
    override fun valueSize(): Int = 1
}

object BoolType : AbiType {
    override fun isDynamic() = false
    override fun isIndexHashed() = false
    override fun valueSize(): Int = 1
}

data class StaticBytesType(val size: Int) : AbiType {
    override fun isDynamic() = false
    override fun isIndexHashed() = false
    override fun valueSize(): Int = 1
}

data class StaticArrayType<T : AbiType>(val elem: T, val size: Int) : AbiType {
    override fun isDynamic() = elem.isDynamic()
    override fun isIndexHashed() = true
    override fun valueSize(): Int = if (isDynamic()) 1 else elem.valueSize() * size
}

data class DynamicArrayType<T : AbiType>(val elem: T) : AbiType {
    override fun isDynamic() = true
    override fun isIndexHashed() = true
    override fun valueSize(): Int = 1
}

object DynamicBytesType : AbiType {
    override fun isDynamic() = true
    override fun isIndexHashed() = true
    override fun valueSize(): Int = 1
}

object StringType : AbiType {
    override fun isDynamic() = true
    override fun isIndexHashed() = true
    override fun valueSize(): Int = 1
}

data class TupleType(val elems: List<AbiType>) : AbiType {
    constructor(vararg elems: AbiType) : this(elems.toList())

    override fun isDynamic() = elems.any { it.isDynamic() }
    override fun isIndexHashed() = true
    override fun valueSize(): Int = if (isDynamic()) 1 else elems.sumOf { it.valueSize() }
}

data class Tuple(val elems: List<Any>)
