package tatskaari.parsing

interface IExpressionVisitor<out NewNodeType> {
  fun visitIntLiteral(intLiteral: Expression.IntLiteral): NewNodeType
  fun visitNumLiteral(numLiteral: Expression.NumLiteral): NewNodeType
  fun visitBoolLiteral(booleanLiteral: Expression.BooleanLiteral): NewNodeType
  fun visitTextLiteral(textLiteral: Expression.TextLiteral): NewNodeType
  fun visitIdentifier(identifier: Expression.Identifier): NewNodeType
  fun visitBinaryOperation(binaryOperation: Expression.BinaryOperation): NewNodeType
  fun visitUnaryOperator(unaryOperation: Expression.UnaryOperation): NewNodeType
  fun visitFunctionCall(functionCall: Expression.FunctionCall): NewNodeType
  fun visitListAccess(listAccess: Expression.ListAccess): NewNodeType
  fun visitListDeclaration(listDeclaration: Expression.ListDeclaration): NewNodeType
  fun visitFunction(function: Expression.Function): NewNodeType
  fun visitConstructorCall(constructorCall: Expression.ConstructorCall): NewNodeType
  fun visitTuple(tuple: Expression.Tuple): NewNodeType
  fun visitMatch(match: Expression.Match): NewNodeType
}
