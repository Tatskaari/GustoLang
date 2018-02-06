package tatskaari.parsing.typechecking

interface ITypedStatementVisitor {
  fun accept(stmt: TypedStatement.Assignment)
  fun accept(stmt: TypedStatement.ValDeclaration)
  fun accept(stmt: TypedStatement.While)
  fun accept(stmt: TypedStatement.CodeBlock)
  fun accept(stmt: TypedStatement.If)
  fun accept(stmt: TypedStatement.IfElse)
  fun accept(stmt: TypedStatement.Return)
  fun accept(stmt: TypedStatement.Output)
  fun accept(stmt: TypedStatement.Input)
  fun accept(stmt: TypedStatement.FunctionDeclaration)
  fun accept(stmt: TypedStatement.ListAssignment)
  fun accept(stmt: TypedStatement.ExpressionStatement)
}