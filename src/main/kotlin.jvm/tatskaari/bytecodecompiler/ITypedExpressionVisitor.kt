package tatskaari.bytecodecompiler

import tatskaari.parsing.TypeChecking.TypedExpression

interface ITypedExpressionVisitor {
  fun visit(expr: TypedExpression.IntLiteral)
  fun visit(expr: TypedExpression.NumLiteral)
  fun visit(expr: TypedExpression.TextLiteral)
  fun visit(expr: TypedExpression.BooleanLiteral)
  fun visit(expr: TypedExpression.Identifier)
  fun visit(expr: TypedExpression.UnaryOperator)
  fun visit(expr: TypedExpression.IntArithmeticOperation)
  fun visit(expr: TypedExpression.NumArithmeticOperation)
  fun visit(expr: TypedExpression.Concatenation)
  fun visit(expr: TypedExpression.LogicalOperation)
  fun visit(expr: TypedExpression.FunctionCall)
  fun visit(expr: TypedExpression.ListDeclaration)
  fun visit(expr: TypedExpression.Function)
  fun visit(expr: TypedExpression.ListAccess)
}