package tatskaari.parsing.typechecking

interface ITypedExpressionVisitor {
  fun visit(expr: TypedExpression.IntLiteral)
  fun visit(expr: TypedExpression.NumLiteral)
  fun visit(expr: TypedExpression.TextLiteral)
  fun visit(expr: TypedExpression.BooleanLiteral)
  fun visit(expr: TypedExpression.Identifier)
  fun visit(expr: TypedExpression.NegateInt)
  fun visit(expr: TypedExpression.NegateNum)
  fun visit(expr: TypedExpression.Not)
  fun visit(expr: TypedExpression.IntArithmeticOperation)
  fun visit(expr: TypedExpression.NumArithmeticOperation)
  fun visit(expr: TypedExpression.Concatenation)
  fun visit(expr: TypedExpression.BooleanLogicalOperation)
  fun visit(expr: TypedExpression.IntLogicalOperation)
  fun visit(expr: TypedExpression.NumLogicalOperation)
  fun visit(expr: TypedExpression.FunctionCall)
  fun visit(expr: TypedExpression.ListDeclaration)
  fun visit(expr: TypedExpression.Function)
  fun visit(expr: TypedExpression.ListAccess)
  fun visit(expr: TypedExpression.Equals)
  fun visit(expr: TypedExpression.NotEquals)
}