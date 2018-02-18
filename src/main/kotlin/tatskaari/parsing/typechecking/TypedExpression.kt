package tatskaari.parsing.typechecking

import tatskaari.GustoType.*
import tatskaari.GustoType
import tatskaari.parsing.Expression

sealed class TypedExpression(val gustoType: GustoType, val expression : Expression) {
  class NumLiteral(val expr: Expression.NumLiteral): TypedExpression(PrimitiveType.Number, expr){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class IntLiteral(val expr: Expression.IntLiteral): TypedExpression(PrimitiveType.Integer, expr){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class TextLiteral(val expr: Expression.TextLiteral): TypedExpression(PrimitiveType.Text, expr){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class BooleanLiteral(val expr: Expression.BooleanLiteral): TypedExpression(PrimitiveType.Boolean, expr){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class Identifier(val expr: Expression.Identifier, type: GustoType): TypedExpression(type, expr){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class NegateInt(val expr: Expression.UnaryOperator, val rhs: TypedExpression): TypedExpression(PrimitiveType.Integer, expr){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class NegateNum(val expr: Expression.UnaryOperator, val rhs: TypedExpression): TypedExpression(PrimitiveType.Number, expr){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class Not(val expr: Expression.UnaryOperator, val rhs: TypedExpression): TypedExpression(PrimitiveType.Boolean, expr){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class IntArithmeticOperation(val expr: Expression.BinaryOperator, val operator: ArithmeticOperator, val lhs: TypedExpression, val rhs: TypedExpression): TypedExpression(PrimitiveType.Integer, expr){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class NumArithmeticOperation(val expr: Expression.BinaryOperator, val operator: ArithmeticOperator, val lhs: TypedExpression, val rhs: TypedExpression): TypedExpression(PrimitiveType.Number, expr){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class Concatenation(val expr: Expression.BinaryOperator, val lhs: TypedExpression, val rhs: TypedExpression): TypedExpression(PrimitiveType.Text, expr){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class NumLogicalOperation(val expr: Expression.BinaryOperator, val operator: NumericLogicalOperator, val lhs: TypedExpression, val rhs: TypedExpression): TypedExpression(PrimitiveType.Boolean, expr){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class IntLogicalOperation(val expr: Expression.BinaryOperator, val operator: NumericLogicalOperator, val lhs: TypedExpression, val rhs: TypedExpression): TypedExpression(PrimitiveType.Boolean, expr){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class Equals(val expr: Expression.BinaryOperator, val lhs: TypedExpression, val rhs: TypedExpression): TypedExpression(PrimitiveType.Boolean, expr){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class NotEquals(val expr: Expression.BinaryOperator, val lhs: TypedExpression, val rhs: TypedExpression): TypedExpression(PrimitiveType.Boolean, expr){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class BooleanLogicalOperation(val expr: Expression.BinaryOperator, val operator: BooleanLogicalOperator, val lhs: TypedExpression, val rhs: TypedExpression): TypedExpression(PrimitiveType.Boolean, expr){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class FunctionCall(val expr: Expression.FunctionCall, val functionExpression: TypedExpression, val paramExprs: List<TypedExpression>, val functionType: FunctionType): TypedExpression(functionType.returnType, expr) {
    override fun accept(visitor: ITypedExpressionVisitor) {
      return visitor.visit(this)
    }
  }

  class ListAccess(val expr: Expression.ListAccess, type: GustoType, val listExpression: TypedExpression, val indexExpr: TypedExpression): TypedExpression(type, expr){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class ListDeclaration(val expr: Expression.ListDeclaration, type: GustoType, val listItemExpr: List<TypedExpression>) : TypedExpression(type, expr){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class Function(val expr: Expression.Function, val body: TypedStatement.CodeBlock, val functionType: FunctionType): TypedExpression(functionType, expr){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class ConstructorCall(val expr: Expression.ConstructorCall, val parameter: TypedExpression?, type: GustoType.VariantMember) : TypedExpression(type, expr) {
    override fun accept(visitor: ITypedExpressionVisitor) {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
  }

  class Tuple(val expr: Expression, type: GustoType) : TypedExpression(type, expr) {
    override fun accept(visitor: ITypedExpressionVisitor) {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
  }

  class Match(val expr: Expression.Match, val matchBranches : List<TypedMatchBranch>, val matchExpr: TypedExpression, val elseBranch: TypedElseBranch, type: GustoType) : TypedExpression(type, expr) {
    override fun accept(visitor: ITypedExpressionVisitor) {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
  }

  abstract fun accept(visitor: ITypedExpressionVisitor)
}