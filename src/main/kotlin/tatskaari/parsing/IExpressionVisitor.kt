package tatskaari.parsing

interface IExpressionVisitor<NewNodeType> {
  fun visit(intLiteral: Expression.IntLiteral): NewNodeType
  fun visit(numLiteral: Expression.NumLiteral): NewNodeType
  fun visit(booleanLiteral: Expression.BooleanLiteral): NewNodeType
  fun visit(textLiteral: Expression.TextLiteral): NewNodeType
  fun visit(identifier: Expression.Identifier): NewNodeType
  fun visit(binaryOperator: Expression.BinaryOperator): NewNodeType
  fun visit(unaryOperator: Expression.UnaryOperator): NewNodeType
  fun visit(functionCall: Expression.FunctionCall): NewNodeType
  fun visit(listAccess: Expression.ListAccess): NewNodeType
  fun visit(listDeclaration: Expression.ListDeclaration): NewNodeType
  fun visit(function: Expression.Function): NewNodeType
  fun visit(constructorCall: Expression.ConstructorCall): NewNodeType
  fun visit(tuple: Expression.Tuple): NewNodeType
  fun visit(match: Expression.Match): NewNodeType
}