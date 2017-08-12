package tatskaari.parsing

import tatskaari.tokenising.Token

sealed class Statement {
  data class CodeBlock(val statementList: List<Statement>) : Statement()
  data class Assignment(val identifier: Token.Identifier, val expression: Expression) : Statement()
  data class If(val condition : Expression, val body : List<Statement>) : Statement()
  data class Input(val ident : Token.Identifier) : Statement()
  data class Output(val expression: Expression) : Statement()
}