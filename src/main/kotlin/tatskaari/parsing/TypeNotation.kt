package tatskaari.parsing

import tatskaari.GustoType

sealed class TypeNotation {
  protected val primitiveTypes = mapOf(
    "integer" to GustoType.PrimitiveType.Integer,
    "number" to GustoType.PrimitiveType.Number,
    "boolean" to GustoType.PrimitiveType.Boolean,
    "text" to GustoType.PrimitiveType.Text
  )

  data class Function(val params: List<TypeNotation>, val returnType: TypeNotation): TypeNotation() {
    override fun toGustoType(env: HashMap<String, GustoType>): GustoType {
      val returnType = returnType.toGustoType(env)
      val paramTypes = params.map { it.toGustoType(env) }
      return GustoType.FunctionType(paramTypes, returnType)
    }
  }

  data class Tuple(val members: List<TypeNotation>): TypeNotation() {
    override fun toGustoType(env: HashMap<String, GustoType>) =
      GustoType.TupleType(members.map {it.toGustoType(env)})
  }

  data class ListOf(val typeNotation: TypeNotation): TypeNotation() {
    override fun toGustoType(env: HashMap<String, GustoType>) = GustoType.ListType(typeNotation.toGustoType(env))
  }

  data class Atomic(val name: String) : TypeNotation() {
    override fun toGustoType(env: HashMap<String, GustoType>) : GustoType{
      return if (primitiveTypes.containsKey(name)){
        primitiveTypes.getValue(name)
      } else {
        return if (env.containsKey(name)){
          env.getValue(name)
        } else {
          GustoType.GenericType(name)
        }
      }
    }
  }

  data class VariantMember(val name: String, val type: TypeNotation): TypeNotation() {
    override fun toGustoType(env: HashMap<String, GustoType>): GustoType.VariantMember {
      return GustoType.VariantMember(name, type.toGustoType(env))
    }
  }

  object UnknownType: TypeNotation() {
    override fun toGustoType(env: HashMap<String, GustoType>) = GustoType.UnknownType
  }

  object Unit: TypeNotation() {
    override fun toGustoType(env: HashMap<String, GustoType>) = GustoType.PrimitiveType.Unit
  }

  abstract fun toGustoType(env: HashMap<String, GustoType>) : GustoType
}