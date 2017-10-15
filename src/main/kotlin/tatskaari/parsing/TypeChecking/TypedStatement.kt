package tatskaari.parsing.TypeChecking

import tatskaari.FunctionType
import tatskaari.GustoType
import tatskaari.PrimitiveType
import tatskaari.parsing.Statement

sealed class TypedStatement(val returnType: GustoType){
  class Assignment(val statement: Statement.Assignment, expression: TypedExpression): TypedStatement(PrimitiveType.Unit)
  class ValDeclaration(val statement: Statement.ValDeclaration, expression: TypedExpression): TypedStatement(PrimitiveType.Unit)
  class While(val statement: Statement.While, val body: TypedStatement, val condition: TypedExpression): TypedStatement(body.returnType)
  class CodeBlock(val statement: Statement.CodeBlock, val body: List<TypedStatement>, returnType: GustoType): TypedStatement(returnType)
  class If(val statement: Statement.If, val body: CodeBlock, val condition: TypedExpression): TypedStatement(body.returnType)
  class IfElse(val statement: Statement.IfElse, val trueBody: CodeBlock, elseBody:CodeBlock, val condition: TypedExpression): TypedStatement(trueBody.returnType)
  class Return(val statement: Statement.Return, val expression: TypedExpression): TypedStatement(expression.gustoType)
  class Output(val statement: Statement.Output, val expression: TypedExpression): TypedStatement(PrimitiveType.Unit)
  class Input(val statement: Statement.Input): TypedStatement(PrimitiveType.Unit)
  class FunctionDeclaration(val statement: Statement.FunctionDeclaration, val body: CodeBlock, val functionType: FunctionType): TypedStatement(PrimitiveType.Unit)
  class ListAssignment(val statement: Statement.ListAssignment, val indexExpression: TypedExpression, val listExpression: TypedExpression): TypedStatement(PrimitiveType.Unit)
}