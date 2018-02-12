package tatskaari

sealed class GustoType {
  object UnknownType: GustoType() {
    override fun toString(): String {
      return "UnknownType"
    }
  }

  sealed class PrimitiveType : GustoType() {
    object Number : PrimitiveType()
    object Integer : PrimitiveType()
    object Text : PrimitiveType()
    object Boolean : PrimitiveType()
    object Unit : PrimitiveType()

    override fun toString(): String {
      return when(this){
        PrimitiveType.Number -> "number"
        PrimitiveType.Integer -> "integer"
        PrimitiveType.Text -> "text"
        PrimitiveType.Boolean -> "boolean"
        PrimitiveType.Unit -> "unit"
      }
    }
  }

  data class GenericType(val name: String) : GustoType() {
    override fun toString(): String {
      return name
    }
  }

  data class VariantMember(val name: String, var type: GustoType) : GustoType() {
    override fun toString() = "$name of $type"
  }
  data class VariantType(val name: String, var members : List<VariantMember>): GustoType() {
    override fun toString() = name
  }

  data class ListType(val type: GustoType): GustoType() {
    override fun toString(): String {
      return type.toString() + " list"
    }
  }

  data class TupleType(val types: List<GustoType>): GustoType() {
    override fun toString(): String {
      return types.joinToString(",", "(", ")")
    }
  }

  data class FunctionType(val params: List<GustoType>, val returnType: GustoType): GustoType() {

    override fun toString(): String {
      return params.joinToString (separator = ", ", transform = { it.toString() }, prefix = "(", postfix = ") ${if (returnType == PrimitiveType.Unit) "" else "-> $returnType"}")
    }
  }
}



