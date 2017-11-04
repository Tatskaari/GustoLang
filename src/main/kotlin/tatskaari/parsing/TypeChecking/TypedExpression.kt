package tatskaari.parsing.TypeChecking

import tatskaari.GustoType.*
import tatskaari.GustoType
import tatskaari.bytecodecompiler.ITypedExpressionVisitor
import tatskaari.parsing.Expression

sealed class TypedExpression(val gustoType: GustoType) {
  class NumLiteral(val expr: Expression.NumLiteral): TypedExpression(PrimitiveType.Number){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class IntLiteral(val expr: Expression.IntLiteral): TypedExpression(PrimitiveType.Integer){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class TextLiteral(val expr: Expression.TextLiteral): TypedExpression(PrimitiveType.Text){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class BooleanLiteral(val expr: Expression.BooleanLiteral): TypedExpression(PrimitiveType.Boolean){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class Identifier(val expr: Expression.Identifier, type: GustoType): TypedExpression(type){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class NegateInt(val expr: Expression.UnaryOperator, val rhs: TypedExpression): TypedExpression(PrimitiveType.Integer){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class NegateNum(val expr: Expression.UnaryOperator, val rhs: TypedExpression): TypedExpression(PrimitiveType.Number){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class Not(val expr: Expression.UnaryOperator, val rhs: TypedExpression): TypedExpression(PrimitiveType.Boolean){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class IntArithmeticOperation(val expr: Expression.BinaryOperator, val operator: ArithmeticOperator, val lhs: TypedExpression, val rhs: TypedExpression): TypedExpression(PrimitiveType.Integer){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class NumArithmeticOperation(val expr: Expression.BinaryOperator, val operator: ArithmeticOperator, val lhs: TypedExpression, val rhs: TypedExpression): TypedExpression(PrimitiveType.Number){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class Concatenation(val expr: Expression.BinaryOperator, val lhs: TypedExpression, val rhs: TypedExpression): TypedExpression(PrimitiveType.Text){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class NumLogicalOperation(val expr: Expression.BinaryOperator, val operator: NumericLogicalOperator, val lhs: TypedExpression, val rhs: TypedExpression): TypedExpression(PrimitiveType.Boolean){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class IntLogicalOperation(val expr: Expression.BinaryOperator, val operator: NumericLogicalOperator, val lhs: TypedExpression, val rhs: TypedExpression): TypedExpression(PrimitiveType.Boolean){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class Equals(val expr: Expression.BinaryOperator, val lhs: TypedExpression, val rhs: TypedExpression): TypedExpression(PrimitiveType.Boolean){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class NotEquals(val expr: Expression.BinaryOperator, val lhs: TypedExpression, val rhs: TypedExpression): TypedExpression(PrimitiveType.Boolean){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class BooleanLogicalOperation(val expr: Expression.BinaryOperator, val operator: BooleanLogicalOperator, val lhs: TypedExpression, val rhs: TypedExpression): TypedExpression(PrimitiveType.Boolean){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class FunctionCall(val expr: Expression.FunctionCall, val functionExpression: TypedExpression, val paramExprs: List<TypedExpression>, val functionType: FunctionType): TypedExpression(functionType.returnType) {
    override fun accept(visitor: ITypedExpressionVisitor) {
      return visitor.visit(this)
    }
  }

  class ListAccess(val expr: Expression.ListAccess, type: GustoType, val listExpression: TypedExpression, val indexExpr: TypedExpression): TypedExpression(type){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class ListDeclaration(val expr: Expression.ListDeclaration, type: GustoType, val listItemExpr: List<TypedExpression>) : TypedExpression(type){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }
  class Function(val expr: Expression.Function, val body: TypedStatement.CodeBlock, val functionType: FunctionType): TypedExpression(functionType){
    override fun accept(visitor: ITypedExpressionVisitor) {
      visitor.visit(this)
    }
  }


  abstract fun accept(visitor: ITypedExpressionVisitor)
}