package parsing

import tokenising.Token


sealed class Statement {
  data class CodeBlock(val statementList: List<Statement>) : Statement()
  data class Assignment(val identifier: Token.Identifier, val expression: Expression) : Statement()
}