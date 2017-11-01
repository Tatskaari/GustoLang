package tatskaari.parsing.TypeChecking

import tatskaari.GustoType
import tatskaari.GustoType.*
import tatskaari.parsing.Statement

sealed class TypedStatement(val returnType: GustoType){
  class Assignment(val statement: Statement.Assignment, val expression: TypedExpression): TypedStatement(PrimitiveType.Unit) {
    override fun accept(visitor: ITypedStatementVisitor) {
      visitor.accept(this)
    }
  }

  class ValDeclaration(val statement: Statement.ValDeclaration, val expression: TypedExpression): TypedStatement(PrimitiveType.Unit){
    override fun accept(visitor: ITypedStatementVisitor) {
      visitor.accept(this)
    }
  }
  class While(val statement: Statement.While, val body: TypedStatement, val condition: TypedExpression): TypedStatement(body.returnType){
    override fun accept(visitor: ITypedStatementVisitor) {
      visitor.accept(this)
    }
  }
  class CodeBlock(val statement: Statement.CodeBlock, val body: List<TypedStatement>, returnType: GustoType): TypedStatement(returnType){
    override fun accept(visitor: ITypedStatementVisitor) {
      visitor.accept(this)
    }
  }
  class If(val statement: Statement.If, val body: CodeBlock, val condition: TypedExpression): TypedStatement(body.returnType){
    override fun accept(visitor: ITypedStatementVisitor) {
      visitor.accept(this)
    }
  }
  class IfElse(val statement: Statement.IfElse, val trueBody: CodeBlock, val elseBody:CodeBlock, val condition: TypedExpression): TypedStatement(trueBody.returnType){
    override fun accept(visitor: ITypedStatementVisitor) {
      visitor.accept(this)
    }
  }
  class Return(val statement: Statement.Return, val expression: TypedExpression): TypedStatement(expression.gustoType){
    override fun accept(visitor: ITypedStatementVisitor) {
      visitor.accept(this)
    }
  }
  class Output(val statement: Statement.Output, val expression: TypedExpression): TypedStatement(PrimitiveType.Unit){
    override fun accept(visitor: ITypedStatementVisitor) {
      visitor.accept(this)
    }
  }
  class Input(val statement: Statement.Input): TypedStatement(PrimitiveType.Unit){
    override fun accept(visitor: ITypedStatementVisitor) {
      visitor.accept(this)
    }
  }
  class FunctionDeclaration(val statement: Statement.FunctionDeclaration, val body: CodeBlock, val functionType: FunctionType): TypedStatement(PrimitiveType.Unit){
    override fun accept(visitor: ITypedStatementVisitor) {
      visitor.accept(this)
    }
  }
  class ListAssignment(val statement: Statement.ListAssignment, val indexExpression: TypedExpression, val listExpression: TypedExpression): TypedStatement(PrimitiveType.Unit){
    override fun accept(visitor: ITypedStatementVisitor) {
      visitor.accept(this)
    }
  }
  class ExpressionStatement(val statement: Statement.ExpressionStatement, val expression: TypedExpression): TypedStatement(PrimitiveType.Unit){
    override fun accept(visitor: ITypedStatementVisitor) {
      visitor.accept(this)
    }
  }

  abstract fun accept(visitor: ITypedStatementVisitor)
}