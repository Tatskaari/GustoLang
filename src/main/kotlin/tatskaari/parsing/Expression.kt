package tatskaari.parsing

import tatskaari.tokenising.Token

data class InvalidOperatorToken(val token : Token) : RuntimeException("Invalid operator $token")

sealed class Expression {
  data class IntLiteral(val value: Int) : Expression()
  data class NumLiteral(val value: Double): Expression()
  data class BooleanLiteral(val value: Boolean) : Expression()
  data class TextLiteral(val value: String) : Expression()
  data class Identifier(val name: String) : Expression()
  data class BinaryOperator(val operator: BinaryOperators, val lhs: Expression, val rhs: Expression) : Expression()
  data class UnaryOperator(val operator: UnaryOperators, val expression: Expression) : Expression()
  data class FunctionCall(val functionExpression: Expression, val params: List<Expression>) : Expression()
  data class ListAccess(val listExpression: Expression, val indexExpression: Expression): Expression()
  data class ListDeclaration(val items: List<Expression>): Expression()
}