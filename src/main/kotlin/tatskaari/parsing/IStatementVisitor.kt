package tatskaari.parsing

interface IStatementVisitor <NewNodeType>{
  fun visit(statement: Statement.ValDeclaration): NewNodeType
  fun visit(statement: Statement.CodeBlock): NewNodeType
  fun visit(statement: Statement.Assignment): NewNodeType
  fun visit(statement: Statement.ListAssignment): NewNodeType
  fun visit(statement: Statement.If): NewNodeType
  fun visit(statement: Statement.IfElse): NewNodeType
  fun visit(statement: Statement.Input): NewNodeType
  fun visit(statement: Statement.Output): NewNodeType
  fun visit(statement: Statement.While): NewNodeType
  fun visit(statement: Statement.FunctionDeclaration): NewNodeType
  fun visit(statement: Statement.Return): NewNodeType
  fun visit(statement: Statement.ExpressionStatement): NewNodeType
  fun visit(typeDeclaration: Statement.TypeDeclaration): NewNodeType
}