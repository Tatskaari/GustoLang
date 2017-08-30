package tatskaari.parsing

import tatskaari.GustoType
import tatskaari.PrimitiveType
import tatskaari.tokenising.Token

sealed class Statement(val startTok: Token, val endTok: Token) {
  data class ValDeclaration(val identifier: Token.Identifier, val expression: Expression, val type: GustoType, val startToken: Token, val endToken: Token) : Statement(startToken, endToken)
  data class CodeBlock(val statementList: List<Statement>, val startToken: Token, val endToken: Token) : Statement(startToken, endToken)
  data class Assignment(val identifier: Token.Identifier, val expression: Expression, val startToken: Token, val endToken: Token) : Statement(startToken, endToken)
  data class ListAssignment(val identifier: Token.Identifier, val indexExpression: Expression, val expression: Expression, val startToken: Token, val endToken: Token) : Statement(startToken, endToken)
  data class If(val condition: Expression, val body: List<Statement>, val startToken: Token, val endToken: Token) : Statement(startToken, endToken)
  data class IfElse(val condition: Expression, val ifBody: List<Statement>, val elseBody: List<Statement>, val startToken: Token, val endToken: Token) : Statement(startToken, endToken)
  data class Input(val identifier: Token.Identifier, val startToken: Token, val endToken: Token) : Statement(startToken, endToken)
  data class Output(val expression: Expression, val startToken: Token, val endToken: Token) : Statement(startToken, endToken)
  data class While(val condition: Expression, val body: CodeBlock, val startToken: Token, val endToken: Token) : Statement(startToken, endToken)
  data class FunctionDeclaration(val identifier: Token.Identifier, val function: Expression.Function, val startToken: Token, val endToken: Token) : Statement(startToken, endToken)
  data class Return(val expression: Expression, val startToken: Token, val endToken: Token) : Statement(startToken, endToken)
}