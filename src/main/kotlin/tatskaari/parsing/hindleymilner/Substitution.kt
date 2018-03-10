package tatskaari.parsing.hindleymilner

import tatskaari.parsing.ASTNode

class Substitution (map : Map<String, Type>) : Map<String, Type> by map {
  fun removeAll(keys : List<String>): Substitution {
    val map = this.toMutableMap()
    keys.forEach { map.remove(it) }
    return Substitution(map)
  }

  fun compose(other: Substitution) : Substitution {
    val map = toMutableMap()
    map.putAll(other.mapValues { it.value.applySubstitution(this) })
    return Substitution(map.mapValues { it.value.applySubstitution(Substitution(map)) })
  }

  fun resolveConstraints(node: ASTNode, errors : MutableList<TypeError>) : Substitution {
    val sub = Substitution(mapValues { it.value.resolveConstraints(node, errors) })
    return sub.compose(sub)
  }


  companion object {
    fun empty() = Substitution(emptyMap())
  }
}