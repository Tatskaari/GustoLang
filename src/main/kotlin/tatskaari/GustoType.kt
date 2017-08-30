package tatskaari

interface GustoType

enum class PrimitiveType : GustoType {
  Number, Integer, Text, Boolean, Unit
}

data class ListType(val type: GustoType?): GustoType {
  override fun equals(other: Any?): Boolean {
    if (other is ListType) {
      if (other.type == type || type == null || other.type == null){
        return true
      }
    }
    return false
  }
}
data class FunctionType(val params: List<GustoType>, val returnType: GustoType): GustoType