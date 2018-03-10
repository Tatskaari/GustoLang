package tatskaari.parsing.hindleymilner

sealed class Type : Substitutable {
  open fun resolveConstraints() = this

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

  data class Var(val name: String, val constraints: Set<Type>) : Type() {
    constructor(name: String) : this(name, setOf())
    override fun applySubstitution(substitution: Substitution): Type {
      return if (substitution.containsKey(name)){
        val type = substitution.getValue(name)
        if (type is Type.Var && type.name == this.name){
          Type.Var(name, type.constraints.union(constraints))
        } else {
          substitution.getValue(name)
        }
      } else {
        this
      }
    }

    override fun resolveConstraints(): Type {
      return constraints.fold(this as Type) { acc, next ->
        when {
          next is Type.ConstrainedType && next.types.contains(acc) -> next
          acc is Type.ConstrainedType && acc.types.contains(next) ->  acc
          acc is Type.Var -> next
          next is Type.Var -> acc
          next == acc -> next
          else -> throw RuntimeException("Types don't match $acc to $next")
        }
      }
    }

    override fun freeTypeVariables() = setOf(name)
    override fun toString() = "var $name"
  }

  data class ConstrainedType(val name: String, val types: Set<Type>) : Type() {
    override fun applySubstitution(substitution: Substitution) =
      ConstrainedType(name, types.map { it.applySubstitution(substitution) }.toSet())

    override fun freeTypeVariables() : Set<String> = emptySet()
    override fun toString() = "$name of $types"

    companion object {
      val numeric = ConstrainedType("numeric", setOf(Type.Int, Type.Num))
    }
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