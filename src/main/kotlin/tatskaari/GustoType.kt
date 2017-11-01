package tatskaari

sealed class GustoType {
  object UnknownType: GustoType()

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

  data class ListType(val type: GustoType): GustoType() {
    override fun equals(other: Any?): Boolean {
      if (other is ListType) {
        if (other.type == type || type == UnknownType || other.type == UnknownType){
          return true
        }
      }
      return false
    }

    override fun toString(): String {
      return type.toString() + " list"
    }
  }
  data class FunctionType(val params: List<GustoType>, val returnType: GustoType): GustoType() {

    override fun toString(): String {
      return params.joinToString (separator = ", ", transform = { it.toString() }, prefix = "(", postfix = ") ${if (returnType == PrimitiveType.Unit) "" else "-> $returnType"}")
    }
  }
}



