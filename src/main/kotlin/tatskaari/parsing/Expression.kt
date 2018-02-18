package tatskaari.parsing

import tatskaari.tokenising.Token

data class InvalidOperatorToken(val token : Token) : RuntimeException("Invalid operator $token")

sealed class Expression(startToken: Token, endToken: Token) : ASTNode(startToken, endToken) {
  class IntLiteral(val value: Int, startTok: Token, endTok: Token) : Expression(startTok, endTok) {
    override fun <NewNodeType> accept(visitor: IExpressionVisitor<NewNodeType>): NewNodeType {
      return visitor.visit(this)
    }
  }
  class NumLiteral(val value: Double, startTok: Token, endTok: Token) : Expression(startTok, endTok){
    override fun <NewNodeType> accept(visitor: IExpressionVisitor<NewNodeType>): NewNodeType {
      return visitor.visit(this)
    }
  }
  class BooleanLiteral(val value: Boolean, startTok: Token, endTok: Token) : Expression(startTok, endTok){
    override fun <NewNodeType> accept(visitor: IExpressionVisitor<NewNodeType>): NewNodeType {
      return visitor.visit(this)
    }
  }
  class TextLiteral(val value: String, startTok: Token, endTok: Token) : Expression(startTok, endTok){
    override fun <NewNodeType> accept(visitor: IExpressionVisitor<NewNodeType>): NewNodeType {
      return visitor.visit(this)
    }
  }
  class Identifier(val name: String, startTok: Token, endTok: Token) : Expression(startTok, endTok){
    override fun <NewNodeType> accept(visitor: IExpressionVisitor<NewNodeType>): NewNodeType {
      return visitor.visit(this)
    }
  }
  class BinaryOperator(val operator: BinaryOperators, val lhs: Expression, val rhs: Expression, startTok: Token, endTok: Token) : Expression(startTok, endTok){
    override fun <NewNodeType> accept(visitor: IExpressionVisitor<NewNodeType>): NewNodeType {
      return visitor.visit(this)
    }
  }
  class UnaryOperator(val operator: UnaryOperators, val expression: Expression, startTok: Token, endTok: Token) : Expression(startTok, endTok){
    override fun <NewNodeType> accept(visitor: IExpressionVisitor<NewNodeType>): NewNodeType {
      return visitor.visit(this)
    }
  }
  class FunctionCall(val functionExpression: Expression, val params: List<Expression>, startTok: Token, endTok: Token) : Expression(startTok, endTok){
    override fun <NewNodeType> accept(visitor: IExpressionVisitor<NewNodeType>): NewNodeType {
      return visitor.visit(this)
    }
  }
  class ListAccess(val listExpression: Expression, val indexExpression: Expression, startTok: Token, endTok: Token) : Expression(startTok, endTok){
    override fun <NewNodeType> accept(visitor: IExpressionVisitor<NewNodeType>): NewNodeType {
      return visitor.visit(this)
    }
  }
  class ListDeclaration(val items: List<Expression>, startTok: Token, endTok: Token) : Expression(startTok, endTok){
    override fun <NewNodeType> accept(visitor: IExpressionVisitor<NewNodeType>): NewNodeType {
      return visitor.visit(this)
    }
  }
  class Function(val returnType: TypeNotation, val params: List<Token.Identifier>, val paramTypes: Map<Token.Identifier, TypeNotation>, val body: Statement.CodeBlock, startTok: Token, endTok: Token) : Expression(startTok, endTok){
    override fun <NewNodeType> accept(visitor: IExpressionVisitor<NewNodeType>): NewNodeType {
      return visitor.visit(this)
    }
  }
  class ConstructorCall(val name: String, val expr : Expression?, startTok: Token, endTok: Token): Expression(startTok, endTok) {
    override fun <NewNodeType> accept(visitor: IExpressionVisitor<NewNodeType>): NewNodeType {
      return visitor.visit(this)
    }
  }

  class Tuple(val params : List<Expression>, startTok: Token, endTok: Token) : Expression(startTok, endTok) {
    override fun <NewNodeType> accept(visitor: IExpressionVisitor<NewNodeType>): NewNodeType {
      return visitor.visit(this)
    }
  }

  class Match(val matchBranches : List<MatchBranch>, val expression: Expression, val elseBranch: ElseMatchBranch, startTok: Token, endTok: Token) : Expression(startTok, endTok) {
    override fun <NewNodeType> accept(visitor: IExpressionVisitor<NewNodeType>): NewNodeType {
      return visitor.visit(this)
    }
  }

  abstract fun <NewNodeType> accept(visitor: IExpressionVisitor<NewNodeType>): NewNodeType
}