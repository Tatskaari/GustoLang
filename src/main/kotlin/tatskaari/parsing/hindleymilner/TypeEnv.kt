package tatskaari.parsing.hindleymilner

import tatskaari.GustoType

data class TypeEnv(val schemes : Map<String, Type.Scheme>){
  companion object {
    val builtInTypes = mapOf(
      GustoType.PrimitiveType.Integer.toString() to Type.Int,
      GustoType.PrimitiveType.Number.toString() to Type.Num,
      GustoType.PrimitiveType.Boolean.toString() to Type.Bool,
      GustoType.PrimitiveType.Text.toString() to Type.Text
    )

    fun empty(): TypeEnv {
      return TypeEnv(mapOf())
    }

    fun withScheme(name: String, scheme: Type.Scheme) = empty().withScheme(name, scheme)
  }

  fun applySubstitution(substitution: Substitution): TypeEnv {
    return TypeEnv(schemes.mapValues {
      it.value.applySubstitution(substitution)
    })
  }

  fun freeTypeVariables(): Set<String> {
    return schemes.values.flatMap { it.bindableVars }.toSet()
  }

  fun remove(name: String) : TypeEnv {
    val map = schemes.toMutableMap()
    map.remove(name)
    return TypeEnv(map)
  }

  fun withScheme(name: String, scheme: Type.Scheme) : TypeEnv {
    val schemes = this.schemes.toMutableMap()
    schemes[name] = scheme
    return TypeEnv(schemes)
  }

  fun generalise(type: Type) : Type.Scheme {
    val vars = type.freeTypeVariables()
      .minus(freeTypeVariables())
      .toList()
    return Type.Scheme(vars, type)
  }
}