package tatskaari

interface GustoType {
  fun getJvmTypeDesc():String
  fun getBoxedJvmTypeDesc(): String
}

object UnknownType: GustoType {
  override fun getJvmTypeDesc(): String {
    throw Exception("Attempted to get JVM type for an unknown type")
  }

  override fun getBoxedJvmTypeDesc(): String {
    return getJvmTypeDesc()
  }
}

sealed class PrimitiveType(val jvmTypeDef: String, val boxedJVMDef: String) : GustoType {
  object Number : PrimitiveType("D", "Ljava/lang/Double;")
  object Integer : PrimitiveType("I", "Ljava/lang/Integer;")
  object Text : PrimitiveType("Ljava/lang/String;", "Ljava/lang/String;")
  object Boolean : PrimitiveType("Z", "Ljava/lang/Boolean;")
  object Unit : PrimitiveType("V", "Ljava/lang/Void;")

  override fun getJvmTypeDesc(): String {
    return jvmTypeDef
  }

  override fun getBoxedJvmTypeDesc(): String {
    return boxedJVMDef
  }

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

data class ListType(val type: GustoType): GustoType {
  override fun equals(other: Any?): Boolean {
    if (other is ListType) {
      if (other.type == type || type == UnknownType || other.type == UnknownType){
        return true
      }
    }
    return false
  }

  override fun getJvmTypeDesc(): String {
    return "[${type.getJvmTypeDesc()}"
  }

  override fun getBoxedJvmTypeDesc(): String {
    return getJvmTypeDesc()
  }
}
data class FunctionType(val params: List<GustoType>, val returnType: GustoType): GustoType {
  override fun getJvmTypeDesc(): String {
    return params.joinToString (separator = "", transform = { it.getJvmTypeDesc() }, prefix = "(", postfix = ")${returnType.getJvmTypeDesc()}")
  }

  override fun getBoxedJvmTypeDesc(): String {
    return params.joinToString (separator = "", transform = { it.getBoxedJvmTypeDesc() }, prefix = "(", postfix = ")${returnType.getBoxedJvmTypeDesc()}")
  }

  override fun toString(): String {
    return params.joinToString (separator = ", ", transform = { it.toString() }, prefix = "(", postfix = ") ${if (returnType == PrimitiveType.Unit) "" else "-> $returnType"}")
  }
}