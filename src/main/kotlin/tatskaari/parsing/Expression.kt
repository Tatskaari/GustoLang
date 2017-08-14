package tatskaari.parsing

import tatskaari.tokenising.Operator

sealed class Expression {
  data class Num(val value: Int) : Expression()
  data class And(val lhs : Expression, val rhs : Expression) : Expression()
  data class Or(val lhs : Expression, val rhs : Expression) : Expression()
  data class Bool(val value: Boolean) : Expression()
  data class Identifier(val name: String) : Expression()
  data class Not(val expr : Expression) : Expression()
  data class Op(val operator: Operator, val lhs: Expression, val rhs: Expression) : Expression()
}