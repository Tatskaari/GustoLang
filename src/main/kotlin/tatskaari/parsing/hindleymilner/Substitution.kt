package tatskaari.parsing.hindleymilner

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

  fun resolveConstraints() : Substitution {
    val sub = Substitution(mapValues { it.value.resolveConstraints() })
    return sub.compose(sub)
  }


  companion object {
    fun empty() = Substitution(emptyMap())
  }
}