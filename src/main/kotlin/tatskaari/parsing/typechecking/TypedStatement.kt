package tatskaari.parsing.typechecking

import tatskaari.GustoType
import tatskaari.GustoType.*
import tatskaari.parsing.Statement

sealed class TypedStatement(val returnType: GustoType?, val stmt: Statement){
  class Assignment(val statement: Statement.Assignment, val expression: TypedExpression): TypedStatement(null, statement) {
    override fun accept(visitor: ITypedStatementVisitor) {
      visitor.accept(this)
    }
  }

  class ValDeclaration(val statement: Statement.ValDeclaration, val expression: TypedExpression): TypedStatement(null, statement){
    override fun accept(visitor: ITypedStatementVisitor) {
      visitor.accept(this)
    }
  }
  class While(val statement: Statement.While, val body: TypedStatement, val condition: TypedExpression): TypedStatement(body.returnType, statement){
    override fun accept(visitor: ITypedStatementVisitor) {
      visitor.accept(this)
    }
  }
  class CodeBlock(val statement: Statement.CodeBlock, val body: List<TypedStatement>, returnType: GustoType?): TypedStatement(returnType, statement){
    override fun accept(visitor: ITypedStatementVisitor) {
      visitor.accept(this)
    }
  }
  class If(val statement: Statement.If, val body: CodeBlock, val condition: TypedExpression): TypedStatement(body.returnType, statement){
    override fun accept(visitor: ITypedStatementVisitor) {
      visitor.accept(this)
    }
  }
  class IfElse(val statement: Statement.IfElse, val trueBody: CodeBlock, val elseBody:CodeBlock, val condition: TypedExpression): TypedStatement(trueBody.returnType, statement){
    override fun accept(visitor: ITypedStatementVisitor) {
      visitor.accept(this)
    }
  }
  class Return(val statement: Statement.Return, val expression: TypedExpression): TypedStatement(expression.gustoType, statement){
    override fun accept(visitor: ITypedStatementVisitor) {
      visitor.accept(this)
    }
  }
  class Output(val statement: Statement.Output, val expression: TypedExpression): TypedStatement(null, statement){
    override fun accept(visitor: ITypedStatementVisitor) {
      visitor.accept(this)
    }
  }
  class Input(val statement: Statement.Input): TypedStatement(null, statement){
    override fun accept(visitor: ITypedStatementVisitor) {
      visitor.accept(this)
    }
  }
  class FunctionDeclaration(val statement: Statement.FunctionDeclaration, val body: CodeBlock, val functionType: FunctionType): TypedStatement(null, statement){
    override fun accept(visitor: ITypedStatementVisitor) {
      visitor.accept(this)
    }
  }
  class ListAssignment(val statement: Statement.ListAssignment, val indexExpression: TypedExpression, val listExpression: TypedExpression): TypedStatement(null, statement){
    override fun accept(visitor: ITypedStatementVisitor) {
      visitor.accept(this)
    }
  }
  class ExpressionStatement(val statement: Statement.ExpressionStatement, val expression: TypedExpression): TypedStatement(null, statement){
    override fun accept(visitor: ITypedStatementVisitor) {
      visitor.accept(this)
    }
  }

  class TypeDeclaration(val statement: Statement.TypeDeclaration, val type: GustoType.VariantType) : TypedStatement(null, statement) {
    override fun accept(visitor: ITypedStatementVisitor) {
      TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
  }

  abstract fun accept(visitor: ITypedStatementVisitor)
}