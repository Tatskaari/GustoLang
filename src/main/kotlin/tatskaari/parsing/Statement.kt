package tatskaari.parsing

import tatskaari.GustoType
import tatskaari.tokenising.Token

sealed class Statement(startToken: Token, endToken: Token): ASTNode(startToken, endToken) {
  class ValDeclaration(val pattern: AssignmentPattern, val expression: Expression, startToken: Token, endToken: Token) : Statement(startToken, endToken) {
    override fun <NewNodeType> accept(visitor: IStatementVisitor<NewNodeType>): NewNodeType {
      return visitor.visit(this)
    }
  }
  class CodeBlock(val statementList: List<Statement>, startToken: Token, endToken: Token) : Statement(startToken, endToken){
    override fun <NewNodeType> accept(visitor: IStatementVisitor<NewNodeType>): NewNodeType {
      return visitor.visit(this)
    }
  }
  class Assignment(val identifier: Token.Identifier, val expression: Expression, startToken: Token, endToken: Token) : Statement(startToken, endToken){
    override fun <NewNodeType> accept(visitor: IStatementVisitor<NewNodeType>): NewNodeType {
      return visitor.visit(this)
    }
  }
  class ListAssignment(val identifier: Token.Identifier, val indexExpression: Expression, val expression: Expression, startToken: Token, endToken: Token) : Statement(startToken, endToken){
    override fun <NewNodeType> accept(visitor: IStatementVisitor<NewNodeType>): NewNodeType {
      return visitor.visit(this)
    }
  }
  class If(val condition: Expression, val body: CodeBlock, startToken: Token, endToken: Token) : Statement(startToken, endToken){
    override fun <NewNodeType> accept(visitor: IStatementVisitor<NewNodeType>): NewNodeType {
      return visitor.visit(this)
    }
  }
  class IfElse(val condition: Expression, val ifBody: CodeBlock, val elseBody: CodeBlock, startToken: Token, endToken: Token) : Statement(startToken, endToken){
    override fun <NewNodeType> accept(visitor: IStatementVisitor<NewNodeType>): NewNodeType {
      return visitor.visit(this)
    }
  }
  class Input(val identifier: Token.Identifier, startToken: Token, endToken: Token) : Statement(startToken, endToken){
    override fun <NewNodeType> accept(visitor: IStatementVisitor<NewNodeType>): NewNodeType {
      return visitor.visit(this)
    }
  }
  class Output(val expression: Expression, startToken: Token, endToken: Token) : Statement(startToken, endToken){
    override fun <NewNodeType> accept(visitor: IStatementVisitor<NewNodeType>): NewNodeType {
      return visitor.visit(this)
    }
  }
  class While(val condition: Expression, val body: CodeBlock, startToken: Token, endToken: Token) : Statement(startToken, endToken){
    override fun <NewNodeType> accept(visitor: IStatementVisitor<NewNodeType>): NewNodeType {
      return visitor.visit(this)
    }
  }
  class FunctionDeclaration(val identifier: Token.Identifier, val function: Expression.Function, startToken: Token, endToken: Token) : Statement(startToken, endToken){
    override fun <NewNodeType> accept(visitor: IStatementVisitor<NewNodeType>): NewNodeType {
      return visitor.visit(this)
    }
  }
  class Return(val expression: Expression, startToken: Token, endToken: Token) : Statement(startToken, endToken){
    override fun <NewNodeType> accept(visitor: IStatementVisitor<NewNodeType>): NewNodeType {
      return visitor.visit(this)
    }
  }

  class ExpressionStatement(val expression: Expression, startToken: Token, endToken: Token): Statement(startToken, endToken){
    override fun <NewNodeType> accept(visitor: IStatementVisitor<NewNodeType>): NewNodeType {
      return visitor.visit(this)
    }
  }

  class TypeDeclaration(val identifier: Token.Identifier, val generics : List<TypeNotation>, val members : List<TypeNotation.VariantMember>, startToken: Token, endToken: Token) : Statement(startToken, endToken) {
    override fun <NewNodeType> accept(visitor: IStatementVisitor<NewNodeType>): NewNodeType {
      return visitor.visit(this)
    }
  }

  abstract fun <NewNodeType> accept(visitor: IStatementVisitor<NewNodeType>): NewNodeType
}