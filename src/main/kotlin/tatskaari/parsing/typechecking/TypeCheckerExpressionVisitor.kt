package tatskaari.parsing.typechecking

import tatskaari.GustoType
import tatskaari.GustoType.*
import tatskaari.parsing.*

class TypeCheckerExpressionVisitor(val env: Env, val typeErrors: Errors) : IExpressionVisitor<TypedExpression> {

  override fun visit(expr: Expression.IntLiteral): TypedExpression {
    return TypedExpression.IntLiteral(expr)
  }

  override fun visit(expr: Expression.NumLiteral): TypedExpression {
    return TypedExpression.NumLiteral(expr)
  }

  override fun visit(expr: Expression.BooleanLiteral): TypedExpression {
    return TypedExpression.BooleanLiteral(expr)
  }

  override fun visit(expr: Expression.TextLiteral): TypedExpression {
    return TypedExpression.TextLiteral(expr)
  }

  override fun visit(expr: Expression.Identifier): TypedExpression {
    return if (env.containsKey(expr.name)){
      TypedExpression.Identifier(expr, env.getValue(expr.name))
    } else {
      typeErrors.add(expr , "Identifier ${expr.name} hasn't been declared yet")
      TypedExpression.Identifier(expr, UnknownType)
    }
  }

  override fun visit(expr: Expression.BinaryOperator): TypedExpression {
    val lhs = expr.lhs.accept(this)
    val rhs = expr.rhs.accept(this)

    val lhsType = lhs.gustoType
    val rhsType = rhs.gustoType

    return when(expr.operator) {
      BinaryOperators.Add, BinaryOperators.Mul, BinaryOperators.Sub, BinaryOperators.Div -> {
        if ((lhsType == PrimitiveType.Text || rhsType == PrimitiveType.Text) && expr.operator == BinaryOperators.Add) {
          TypedExpression.Concatenation(expr, lhs, rhs)
        } else if (lhsType == PrimitiveType.Integer && rhsType == PrimitiveType.Integer) {
          TypedExpression.IntArithmeticOperation(expr, ArithmeticOperator.valueOf(expr.operator.name), lhs, rhs)
        } else if ((lhsType == PrimitiveType.Number || lhsType == PrimitiveType.Integer) && (rhsType == PrimitiveType.Number || rhsType == PrimitiveType.Integer)) {
          TypedExpression.NumArithmeticOperation(expr, ArithmeticOperator.valueOf(expr.operator.name), lhs, rhs)
        } else {
          typeErrors.addBinaryOperatorTypeError(expr, expr.operator, lhsType, rhsType)
          TypedExpression.NumArithmeticOperation(expr, ArithmeticOperator.valueOf(expr.operator.name), lhs, rhs)
        }
      }
      BinaryOperators.And, BinaryOperators.Or -> {
        if (lhsType == PrimitiveType.Boolean && rhsType == PrimitiveType.Boolean) {
          TypedExpression.BooleanLogicalOperation(expr, BooleanLogicalOperator.valueOf(expr.operator.name), lhs, rhs)
        } else {
          typeErrors.addBinaryOperatorTypeError(expr, expr.operator, lhsType, rhsType)
          TypedExpression.BooleanLogicalOperation(expr, BooleanLogicalOperator.valueOf(expr.operator.name), lhs, rhs)
        }
      }
      BinaryOperators.Equality -> TypedExpression.Equals(expr, lhs, rhs)
      BinaryOperators.NotEquality -> TypedExpression.NotEquals(expr, lhs, rhs)
      BinaryOperators.GreaterThan, BinaryOperators.GreaterThanEq, BinaryOperators.LessThan, BinaryOperators.LessThanEq -> {
        if ((lhsType == PrimitiveType.Number || lhsType == PrimitiveType.Integer) && (rhsType == PrimitiveType.Number || rhsType == PrimitiveType.Integer)) {
          if (lhs.gustoType == PrimitiveType.Number || rhs.gustoType == PrimitiveType.Number){
            TypedExpression.NumLogicalOperation(expr, NumericLogicalOperator.valueOf(expr.operator.name), lhs, rhs)
          } else {
            TypedExpression.IntLogicalOperation(expr, NumericLogicalOperator.valueOf(expr.operator.name), lhs, rhs)
          }
        } else {
          typeErrors.addBinaryOperatorTypeError(expr, expr.operator, lhsType, rhsType)
          TypedExpression.NumLogicalOperation(expr, NumericLogicalOperator.valueOf(expr.operator.name), lhs, rhs)
        }
      }
    }
  }

  override fun visit(expr: Expression.UnaryOperator): TypedExpression {
    val expressionType = expr.expression.accept(this)

    when(expr.operator){
      UnaryOperators.Negative -> {
        return when {
          expressionType.gustoType == PrimitiveType.Integer -> TypedExpression.NegateInt(expr, expressionType)
          expressionType.gustoType == PrimitiveType.Number -> TypedExpression.NegateNum(expr, expressionType)
          else -> {
            typeErrors.addUnaryOperatorTypeError(expr, expr.operator, expressionType.gustoType)
            TypedExpression.NegateNum(expr, expressionType)
          }
        }
      }
      UnaryOperators.Not -> {
        if (expressionType.gustoType != PrimitiveType.Boolean) {
          typeErrors.addUnaryOperatorTypeError(expr, expr.operator, expressionType.gustoType)
        }
        return TypedExpression.Not(expr, expressionType)
      }
    }
  }

  private fun compareGenerics(genericType: GenericType, exprType: GustoType, genericTypes: HashMap<GenericType, GustoType>): Boolean{
    return if (genericTypes.containsKey(genericType)){
      val expectedType = genericTypes.getValue(genericType)

      if (expectedType is GenericType){
        compareGenerics(expectedType, exprType, genericTypes)
      } else {
        exprType == expectedType
      }
    } else {
      genericTypes.put(genericType, exprType)
      true
    }
  }

  private fun comparePrimitiveType(expectedType: PrimitiveType, actualType: GustoType): Boolean {
    return expectedType == actualType
  }

  private fun compareFunctionType(expectedType: FunctionType, actualType: GustoType, genericTypes: HashMap<GenericType, GustoType>): Boolean{
    if (actualType is FunctionType){
      expectedType.params.zip(actualType.params).forEach{(expected, actual) ->
        if(!compareTypes(expected, actual, genericTypes)){
          return false
        }
      }
      return compareTypes(expectedType.returnType, actualType.returnType, genericTypes)
    } else {
      return false
    }
  }

  private fun compareListType(expectedType: ListType, actualType: GustoType, genericTypes: HashMap<GenericType, GustoType>): Boolean{
    if (actualType is ListType){
      return compareTypes(expectedType.type, actualType.type, genericTypes)
    }
    return false
  }

  private fun compareTypes(expectedType: GustoType, actualType: GustoType, genericTypes: HashMap<GenericType, GustoType>): Boolean{
    return when(expectedType){
      is FunctionType -> compareFunctionType(expectedType, actualType, genericTypes)
      is ListType -> compareListType(expectedType, actualType, genericTypes)
      is GenericType -> compareGenerics(expectedType, actualType, genericTypes)
      is PrimitiveType -> comparePrimitiveType(expectedType, actualType)
      is GustoType.UnknownType -> return true
    }
  }

  private fun expandGenerics(genericType: GenericType, genericTypes: HashMap<GenericType, GustoType>): GustoType{
    return genericTypes.getOrDefault(genericType, genericType)
  }

  private fun expandFunctionType(expectedType: FunctionType, genericTypes: HashMap<GenericType, GustoType>): FunctionType{
    val params = expectedType.params.map {
      expandTypes(it, genericTypes)
    }
    val returnType = expandTypes(expectedType.returnType, genericTypes)
    return FunctionType(params, returnType)
  }

  private fun expandListType(expectedType: ListType, genericTypes: HashMap<GenericType, GustoType>): ListType{
    val type = expandTypes(expectedType.type, genericTypes)
    return ListType(type)
  }

  private fun expandTypes(expectedType: GustoType, genericTypes: HashMap<GenericType, GustoType>): GustoType{
    return when(expectedType){
      is FunctionType -> expandFunctionType(expectedType, genericTypes)
      is ListType -> expandListType(expectedType, genericTypes)
      is GenericType -> expandGenerics(expectedType, genericTypes)
      is PrimitiveType -> expectedType
      is GustoType.UnknownType -> UnknownType
    }
  }

  override fun visit(expr: Expression.FunctionCall): TypedExpression {
    val functionExpr = expr.functionExpression.accept(this)
    val functionType = functionExpr.gustoType
    val params = ArrayList<TypedExpression>()

    val genericTypes = HashMap<GenericType, GustoType>()

    return if (functionType is FunctionType){
      expr.params.zip(functionType.params).forEach { (paramExpr, type) ->
        val typedExpr = paramExpr.accept(this)
        params.add(typedExpr)
        if (!compareTypes(type, typedExpr.gustoType, genericTypes)){
          typeErrors.addTypeMissmatch(expr, type, typedExpr.gustoType)
        }
      }

      TypedExpression.FunctionCall(expr, functionExpr, params, expandFunctionType(functionType, genericTypes))
    } else {
      typeErrors.add(expr, "Unexpected type for target of a function call. Expected function, found $functionType")
      TypedExpression.FunctionCall(expr, functionExpr, params, FunctionType(listOf(), UnknownType))
    }
  }

  override fun visit(expr: Expression.ListAccess): TypedExpression {
    val listExpr = expr.listExpression.accept(this)
    val indexExpr = expr.indexExpression.accept(this)
    val listType = listExpr.gustoType


    return if ((listType is ListType && listType.type != UnknownType)){
      TypedExpression.ListAccess(expr, listType.type, listExpr, indexExpr)
    } else {
      // TODO improve this error message
      typeErrors.add(expr, "Expected list, found $listType")
      TypedExpression.ListAccess(expr, UnknownType, listExpr, indexExpr)
    }
  }

  override fun visit(expr: Expression.ListDeclaration): TypedExpression {
    return if (expr.items.isEmpty()){
      TypedExpression.ListDeclaration(expr, ListType(UnknownType), listOf())
    } else {
      val typedExpressions = ArrayList<TypedExpression>()
      val typedExpression = expr.items[0].accept(this)
      expr.items.forEach {
        val expressionType = it.accept(this)
        typedExpressions.add(expressionType)
        if (expressionType.gustoType != typedExpression.gustoType){
          typeErrors.addTypeMissmatch(expr, typedExpression.gustoType, expressionType.gustoType)
        }
      }
      TypedExpression.ListDeclaration(expr, ListType(typedExpression.gustoType), typedExpressions)
    }
  }

  override fun visit(expr: Expression.Function): TypedExpression {
    val functionEnv = HashMap(env)
    val paramTypes = expr.params.map {
      expr.paramTypes.getValue(it)
    }

    functionEnv.putAll(expr.paramTypes.mapKeys { it.key.name })
    val functionType = FunctionType(paramTypes, expr.returnType)
    val body = expr.body.accept(TypeCheckerStatementVisitor(functionEnv, typeErrors, functionType.returnType)) as TypedStatement.CodeBlock

    if (body.body.isEmpty() && functionType.returnType != PrimitiveType.Unit){
      typeErrors.add(expr, "Missing return")
    }

    ReturnTypeChecker(typeErrors).codeblock(body, functionType.returnType != PrimitiveType.Unit)

    return TypedExpression.Function(expr, body, functionType)
  }

}