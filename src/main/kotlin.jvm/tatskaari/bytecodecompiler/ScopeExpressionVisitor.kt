package tatskaari.bytecodecompiler

import tatskaari.parsing.typechecking.ITypedExpressionVisitor
import tatskaari.parsing.typechecking.TypedExpression
import java.util.*

class ScopeExpressionVisitor(val declaredVariables: LinkedList<String> = LinkedList(), val undeclaredVariables: LinkedList<String> = LinkedList()) : ITypedExpressionVisitor {
  override fun visit(expr: TypedExpression.NumLiteral) {}

  override fun visit(expr: TypedExpression.TextLiteral) {}

  override fun visit(expr: TypedExpression.BooleanLiteral) {}

  override fun visit(expr: TypedExpression.Identifier) {
    if (!declaredVariables.contains(expr.expr.name)){
      undeclaredVariables.add(expr.expr.name)
    }
  }

  override fun visit(expr: TypedExpression.NegateInt) {
    expr.rhs.accept(this)
  }

  override fun visit(expr: TypedExpression.NegateNum) {
    expr.rhs.accept(this)
  }

  override fun visit(expr: TypedExpression.Not) {
    expr.rhs.accept(this)
  }

  override fun visit(expr: TypedExpression.IntArithmeticOperation) {
    expr.rhs.accept(this)
    expr.lhs.accept(this)
  }

  override fun visit(expr: TypedExpression.NumArithmeticOperation) {
    expr.rhs.accept(this)
    expr.lhs.accept(this)  }

  override fun visit(expr: TypedExpression.Concatenation) {
    expr.rhs.accept(this)
    expr.lhs.accept(this)
  }

  override fun visit(expr: TypedExpression.BooleanLogicalOperation) {
    expr.rhs.accept(this)
    expr.lhs.accept(this)
  }

  override fun visit(expr: TypedExpression.IntLogicalOperation) {
    expr.rhs.accept(this)
    expr.lhs.accept(this)
  }

  override fun visit(expr: TypedExpression.NumLogicalOperation) {
    expr.rhs.accept(this)
    expr.lhs.accept(this)
  }

  override fun visit(expr: TypedExpression.FunctionCall) {
    expr.functionExpression.accept(this)
    expr.paramExprs.forEach{it.accept(this)}
  }

  override fun visit(expr: TypedExpression.ListDeclaration) {
    expr.listItemExpr.forEach{it.accept(this)}
  }

  override fun visit(expr: TypedExpression.Function) {
    val subFunctionVisitor = ScopeStatementVisitor()
    expr.expr.paramTypes.keys.forEach{subFunctionVisitor.declaredVariables.add(it.name)}
    expr.body.accept(subFunctionVisitor)
    undeclaredVariables.addAll(subFunctionVisitor.undeclaredVariables.minus(declaredVariables))
  }

  override fun visit(expr: TypedExpression.ListAccess) {
    expr.indexExpr.accept(this)
    expr.listExpression.accept(this)
  }

  override fun visit(expr: TypedExpression.Equals) {
    expr.rhs.accept(this)
    expr.lhs.accept(this)
  }

  override fun visit(expr: TypedExpression.NotEquals) {
    expr.rhs.accept(this)
    expr.lhs.accept(this)
  }

  override fun visit(expr: TypedExpression.IntLiteral) {}

  fun findUndeclaredVariables(expr: TypedExpression.Function): List<String> {
    expr.accept(this)
    return undeclaredVariables
  }
}