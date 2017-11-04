package tatskaari.bytecodecompiler

import tatskaari.parsing.TypeChecking.TypedExpression
import java.util.*

class ScopeExpressionVisitor(val declaredVariables: LinkedList<String>, val undeclaredVariables: LinkedList<String>) : ITypedExpressionVisitor {
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
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
  }

  override fun visit(expr: TypedExpression.Function) {
    TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
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
}