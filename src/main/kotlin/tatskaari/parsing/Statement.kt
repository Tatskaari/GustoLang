package tatskaari.parsing

import tatskaari.tokenising.Token

sealed class Statement {
  abstract class ValDeclaration(val identifier: Token.Identifier, val expression: Expression) : Statement() {
    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other !is ValDeclaration) return false

      if (identifier != other.identifier) return false
      if (expression != other.expression) return false

      return true
    }

    override fun hashCode(): Int {
      var result = identifier.hashCode()
      result = 31 * result + expression.hashCode()
      return result
    }
  }
  data class CodeBlock(val statementList: List<Statement>) : Statement()
  class NumberDeclaration(identifier: Token.Identifier, expression: Expression) : ValDeclaration(identifier, expression)
  class IntegerDeclaration(identifier: Token.Identifier, expression: Expression) : ValDeclaration(identifier, expression)
  class BooleanDeclaration(identifier: Token.Identifier, expression: Expression) : ValDeclaration(identifier, expression)
  class ListDeclaration(identifier: Token.Identifier, expression: Expression) : ValDeclaration(identifier, expression)
  class TextDeclaration(identifier: Token.Identifier, expression: Expression) : ValDeclaration(identifier, expression)
  data class Assignment(val identifier: Token.Identifier, val expression: Expression) : Statement()
  data class ListAssignment(val identifier: Token.Identifier, val indexExpression: Expression, val expression: Expression) : Statement()
  data class If(val condition: Expression, val body: List<Statement>) : Statement()
  data class IfElse(val condition: Expression, val ifBody: List<Statement>, val elseBody: List<Statement>) : Statement()
  data class Input(val identifier: Token.Identifier) : Statement()
  data class Output(val expression: Expression) : Statement()
  data class While(val condition: Expression, val body: CodeBlock) : Statement()
  data class Function(val identifier: Token.Identifier, val params: List<Token.Identifier>, val body: CodeBlock) : Statement()
  data class Return(val expression: Expression) : Statement()
}