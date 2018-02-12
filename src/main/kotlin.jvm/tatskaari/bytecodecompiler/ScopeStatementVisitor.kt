package tatskaari.bytecodecompiler

import tatskaari.parsing.AssignmentPattern
import tatskaari.parsing.typechecking.ITypedStatementVisitor
import tatskaari.parsing.typechecking.TypedStatement
import java.util.*

class ScopeStatementVisitor : ITypedStatementVisitor {
  val declaredVariables = LinkedList<String>()
  val undeclaredVariables = LinkedList<String>()
  val expressionVisitor = ScopeExpressionVisitor(declaredVariables, undeclaredVariables)

  override fun accept(stmt: TypedStatement.Assignment) {
    stmt.expression.accept(expressionVisitor)
    val identifier = stmt.statement.identifier.name
    if (!declaredVariables.contains(identifier)){
      undeclaredVariables.add(identifier)
    }
  }

  override fun accept(stmt: TypedStatement.ValDeclaration) {
    stmt.expression.accept(expressionVisitor)
    //TODO other patterns
    declaredVariables.add((stmt.statement.pattern as AssignmentPattern.Variable).identifier.name)
  }

  override fun accept(stmt: TypedStatement.While) {
    stmt.condition.accept(expressionVisitor)
    stmt.body.accept(this)
  }

  override fun accept(stmt: TypedStatement.CodeBlock) {
    stmt.body.forEach{it.accept(this)}
  }

  override fun accept(stmt: TypedStatement.If) {
    stmt.condition.accept(expressionVisitor)
    stmt.body.accept(this)
  }

  override fun accept(stmt: TypedStatement.IfElse) {
    stmt.condition.accept(expressionVisitor)
    stmt.trueBody.accept(this)
    stmt.elseBody.accept(this)
  }

  override fun accept(stmt: TypedStatement.Return) {
    stmt.expression.accept(expressionVisitor)
  }

  override fun accept(stmt: TypedStatement.Output) {
    stmt.expression.accept(expressionVisitor)
  }

  override fun accept(stmt: TypedStatement.Input) {
    declaredVariables.add(stmt.statement.identifier.name)
  }

  override fun accept(stmt: TypedStatement.FunctionDeclaration) {
    declaredVariables.add(stmt.statement.identifier.name)
    val subFunctionVisitor = ScopeStatementVisitor()
    stmt.statement.function.paramTypes.keys.forEach{subFunctionVisitor.declaredVariables.add(it.name)}
    stmt.body.accept(subFunctionVisitor)
    undeclaredVariables.addAll(subFunctionVisitor.undeclaredVariables.minus(declaredVariables))
  }

  override fun accept(stmt: TypedStatement.ListAssignment) {
    stmt.listExpression.accept(expressionVisitor)
    stmt.indexExpression.accept(expressionVisitor)
  }

  override fun accept(stmt: TypedStatement.ExpressionStatement) {
    stmt.expression.accept(expressionVisitor)
  }

  fun findUndeclaredVars(stmt: TypedStatement.FunctionDeclaration): List<String> {
    stmt.accept(this)
    return undeclaredVariables
  }

}