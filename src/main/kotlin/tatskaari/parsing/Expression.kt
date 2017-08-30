package tatskaari.parsing

import tatskaari.GustoType
import tatskaari.tokenising.Token

data class InvalidOperatorToken(val token : Token) : RuntimeException("Invalid operator $token")

sealed class Expression(val startToken: Token, val endToken: Token) {
  data class IntLiteral(val value: Int, val startTok: Token, val endTok: Token) : Expression(startTok, endTok)
  data class NumLiteral(val value: Double, val startTok: Token, val endTok: Token) : Expression(startTok, endTok)
  data class BooleanLiteral(val value: Boolean, val startTok: Token, val endTok: Token) : Expression(startTok, endTok)
  data class TextLiteral(val value: String, val startTok: Token, val endTok: Token) : Expression(startTok, endTok)
  data class Identifier(val name: String, val startTok: Token, val endTok: Token) : Expression(startTok, endTok)
  data class BinaryOperator(val operator: BinaryOperators, val lhs: Expression, val rhs: Expression, val startTok: Token, val endTok: Token) : Expression(startTok, endTok)
  data class UnaryOperator(val operator: UnaryOperators, val expression: Expression, val startTok: Token, val endTok: Token) : Expression(startTok, endTok)
  data class FunctionCall(val functionExpression: Expression, val params: List<Expression>, val startTok: Token, val endTok: Token) : Expression(startTok, endTok)
  data class ListAccess(val listExpression: Expression, val indexExpression: Expression, val startTok: Token, val endTok: Token) : Expression(startTok, endTok)
  data class ListDeclaration(val items: List<Expression>, val startTok: Token, val endTok: Token) : Expression(startTok, endTok)
  data class Function(val returnType: GustoType, val params: List<Token.Identifier>, val paramTypes: Map<Token.Identifier, GustoType>, val body: Statement.CodeBlock, val startTok: Token, val endTok: Token) : Expression(startTok, endTok)
}