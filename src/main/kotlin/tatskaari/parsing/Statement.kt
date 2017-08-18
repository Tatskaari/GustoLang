package tatskaari.parsing

import tatskaari.tokenising.Token

sealed class Statement {
  data class CodeBlock(val statementList: List<Statement>) : Statement()
  data class ValDeclaration(val identifier: Token.Identifier, val expression: Expression) : Statement()
  data class Assignment(val identifier: Token.Identifier, val expression: Expression) : Statement()
  data class ListAssignment(val identifier: Token.Identifier, val indexExpression: Expression, val expression: Expression) : Statement()
  data class If(val condition: Expression, val body: CodeBlock) : Statement()
  data class IfElse(val condition: Expression, val ifBody: CodeBlock, val elseBody: CodeBlock) : Statement()
  data class Input(val identifier: Token.Identifier) : Statement()
  data class Output(val expression: Expression) : Statement()
  data class While(val condition: Expression, val body: CodeBlock) : Statement()
  data class Function(val identifier: Token.Identifier, val params: List<Token.Identifier>, val body: CodeBlock) : Statement()
  data class Return(val expression: Expression) : Statement()
}