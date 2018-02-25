package tatskaari.parsing.hindleymilner

sealed class Type : Substitutable {
  data class Function(val lhs : Type, val rhs: Type) : Type() {
    override fun applySubstitution(substitution: Substitution): Function =
      Function(lhs.applySubstitution(substitution), rhs.applySubstitution(substitution))

    override fun freeTypeVariables() = lhs.freeTypeVariables().union(rhs.freeTypeVariables())

    fun getReturnType() : Type {
      return if (rhs is Function){
        rhs.getReturnType()
      } else {
        rhs
      }
    }

    override fun toString(): String {
      return if (lhs is Function){
        "($lhs) -> $rhs"
      } else {
        "$lhs -> $rhs"
      }
    }
  }

  data class Var(val name: String) : Type() {
    override fun applySubstitution(substitution: Substitution): Type {
      return if (substitution.containsKey(name)){
        substitution.getValue(name)
      } else {
        this
      }
    }

    override fun freeTypeVariables() = setOf(name)
    override fun toString() = "var $name"
  }

  object Int : Type() {
    override fun applySubstitution(substitution: Substitution) = this
    override fun freeTypeVariables() = setOf<String>()
    override fun toString() = "integer"
  }

  object Num : Type() {
    override fun applySubstitution(substitution: Substitution) = this
    override fun freeTypeVariables() = setOf<String>()
    override fun toString() = "number"

  }

  object Text : Type() {
    override fun applySubstitution(substitution: Substitution) = this
    override fun freeTypeVariables() = setOf<String>()
    override fun toString() = "text"
  }

  object Bool : Type() {
    override fun applySubstitution(substitution: Substitution) = this
    override fun freeTypeVariables() = setOf<String>()
    override fun toString() = "bool"
  }

  object Unit : Type(){
    override fun applySubstitution(substitution: Substitution) = this
    override fun freeTypeVariables() = setOf<String>()
    override fun toString() = "unit"
  }

  data class ListType(val type: Type) : Type() {
    override fun freeTypeVariables() = type.freeTypeVariables()
    override fun applySubstitution(substitution: Substitution)= ListType(type.applySubstitution(substitution))
    override fun toString() = "$type list"
  }

  data class Tuple(val types: List<Type>) : Type(){
    override fun applySubstitution(substitution: Substitution) = Tuple(types.map { it.applySubstitution(substitution) })
    override fun freeTypeVariables() = types.flatMap { it.freeTypeVariables() }.toSet()
    override fun toString() = types.joinToString(", " , "(", ")")
  }

  data class Scheme (val bindableVars: List<String>, val type: Type) : Type() {
    override fun applySubstitution(substitution: Substitution): Scheme {
      return Scheme(bindableVars, type.applySubstitution(substitution.removeAll(bindableVars)))
    }

    override fun freeTypeVariables() = type.freeTypeVariables().minus(bindableVars.toSet())

    override fun toString() = "$bindableVars : $type"
  }
}