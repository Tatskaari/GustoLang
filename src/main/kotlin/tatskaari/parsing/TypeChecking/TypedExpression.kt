package tatskaari.parsing.TypeChecking

import tatskaari.GustoType
import tatskaari.PrimitiveType
import tatskaari.parsing.Expression

sealed class TypedExpression(val gustoType: GustoType) {
  class NumLiteral(val expr: Expression.NumLiteral): TypedExpression(PrimitiveType.Number)
  class IntLiteral(val expr: Expression.IntLiteral): TypedExpression(PrimitiveType.Integer)
  class TextLiteral(val expr: Expression.TextLiteral): TypedExpression(PrimitiveType.Text)
  class BooleanLiteral(val expr: Expression.BooleanLiteral): TypedExpression(PrimitiveType.Boolean)
  class Identifier(val expr: Expression.Identifier, type: GustoType): TypedExpression(type)
  class UnaryOperator(val expr: Expression.UnaryOperator, val rhs: TypedExpression, type: GustoType): TypedExpression(type)
  class BinaryOperator(val expr: Expression.BinaryOperator, val lhs: TypedExpression, val rhs: TypedExpression, type: GustoType): TypedExpression(type)
  class FunctionCall(val expr: Expression.FunctionCall, val functionExpression: TypedExpression, val paramExprs: List<TypedExpression>, type: GustoType): TypedExpression(type)
  class ListAccess(val expr: Expression.ListAccess, type: GustoType, val listExpression: TypedExpression, val indexExpr: TypedExpression): TypedExpression(type)
  class ListDeclaration(val expr: Expression.ListDeclaration, type: GustoType, val listItemExpr: List<TypedExpression>) : TypedExpression(type)
  //TODO include the type of the body
  class Function(val expr: Expression.Function, type: GustoType): TypedExpression(type)
}