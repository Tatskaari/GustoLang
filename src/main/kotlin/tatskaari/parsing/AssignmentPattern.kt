package tatskaari.parsing

import tatskaari.GustoType
import tatskaari.tokenising.Token

sealed class AssignmentPattern {
  data class Variable(val identifier: Token.Identifier, val typeNotation: TypeNotation) : AssignmentPattern() {
    override fun toGustoType(env: HashMap<String, GustoType>) : GustoType {
      return typeNotation.toGustoType(env)
    }
  }

  data class Tuple(val identifiers : List<AssignmentPattern>) : AssignmentPattern() {
    override fun toGustoType(env: HashMap<String, GustoType>): GustoType {
      return GustoType.TupleType(identifiers.map { it.toGustoType(env) })
    }
  }

  data class Constructor(val name : Token.Constructor, val pattern: AssignmentPattern) : AssignmentPattern() {
    override fun toGustoType(env: HashMap<String, GustoType>): GustoType {
      return env[name.name]!!
    }
  }

  abstract fun toGustoType(env: HashMap<String, GustoType>): GustoType
}