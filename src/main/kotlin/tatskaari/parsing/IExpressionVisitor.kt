package tatskaari.parsing

interface IExpressionVisitor<NewNodeType> {
  fun visit(expr: Expression.IntLiteral): NewNodeType
  fun visit(expr: Expression.NumLiteral): NewNodeType
  fun visit(expr: Expression.BooleanLiteral): NewNodeType
  fun visit(expr: Expression.TextLiteral): NewNodeType
  fun visit(expr: Expression.Identifier): NewNodeType
  fun visit(expr: Expression.BinaryOperator): NewNodeType
  fun visit(expr: Expression.UnaryOperator): NewNodeType
  fun visit(expr: Expression.FunctionCall): NewNodeType
  fun visit(expr: Expression.ListAccess): NewNodeType
  fun visit(expr: Expression.ListDeclaration): NewNodeType
  fun visit(expr: Expression.Function): NewNodeType
}