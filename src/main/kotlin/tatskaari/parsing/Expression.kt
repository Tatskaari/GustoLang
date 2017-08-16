package tatskaari.parsing

import tatskaari.tokenising.IToken
import tatskaari.tokenising.Token
data class InvalidOperatorToken(val token : IToken) : RuntimeException("Invalid operator $token")

sealed class Expression {
  data class Num(val value: Int) : Expression()
  data class Bool(val value: Boolean) : Expression()
  data class Identifier(val name: String) : Expression()
  data class BinaryOperator(val operator: BinaryOperators, val lhs: Expression, val rhs: Expression) : Expression()
  data class UnaryOperator(val operator: UnaryOperators, val expression: Expression) : Expression()
  data class FunctionCall(val functionIdentifier: Token.Identifier, val params: List<Expression>) : Expression()
}