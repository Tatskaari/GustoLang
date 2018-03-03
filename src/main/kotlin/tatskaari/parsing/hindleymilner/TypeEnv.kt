package tatskaari.parsing.hindleymilner

import tatskaari.GustoType

data class TypeEnv(val schemes : Map<String, Type.Scheme>, val definedTypes: Map<String, Type>){
  companion object {
    private val builtInTypes = mapOf(
      GustoType.PrimitiveType.Integer.toString() to Type.Int,
      GustoType.PrimitiveType.Number.toString() to Type.Num,
      GustoType.PrimitiveType.Boolean.toString() to Type.Bool,
      GustoType.PrimitiveType.Text.toString() to Type.Text,
      "numeric" to Type.ConstrainedType.numeric
    )

    fun empty(): TypeEnv {
      return TypeEnv(mapOf(), builtInTypes)
    }

    fun withScheme(name: String, scheme: Type.Scheme) = empty().withScheme(name, scheme)
  }

  fun applySubstitution(substitution: Substitution): TypeEnv {
    return TypeEnv(schemes.mapValues {
      it.value.applySubstitution(substitution)
    }, definedTypes)
  }

  private fun freeTypeVariables(): Set<String> {
    return schemes.values.flatMap { it.bindableVars }.toSet()
  }

  fun remove(name: String) : TypeEnv {
    val map = schemes.toMutableMap()
    map.remove(name)
    return TypeEnv(map, definedTypes)
  }

  fun withScheme(name: String, scheme: Type.Scheme) : TypeEnv {
    val schemes = this.schemes.toMutableMap()
    schemes[name] = scheme
    return TypeEnv(schemes, definedTypes)
  }

  fun generalise(type: Type) : Type.Scheme {
    val vars = type.freeTypeVariables()
      .minus(freeTypeVariables())
      .toList()
    return Type.Scheme(vars, type)
  }
}