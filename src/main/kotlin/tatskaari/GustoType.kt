package tatskaari

interface GustoType {
  fun getJvmTypeDesc():String
}

enum class PrimitiveType(val jvmTypeDef: String) : GustoType {
  Number("D"), Integer("I"), Text("Ljava/lang/String;"), Boolean("I"), Unit("V");

  override fun getJvmTypeDesc(): String {
    return jvmTypeDef
  }
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

  override fun getJvmTypeDesc(): String {
    return "[${type!!.getJvmTypeDesc()}"
  }
}
data class FunctionType(val params: List<GustoType>, val returnType: GustoType): GustoType {
  override fun getJvmTypeDesc(): String {
    //TODO make this return a class that implements this function
    return params.joinToString (separator = ";", transform = { it.getJvmTypeDesc() }, prefix = "(", postfix = ")${returnType.getJvmTypeDesc()}")
  }
}