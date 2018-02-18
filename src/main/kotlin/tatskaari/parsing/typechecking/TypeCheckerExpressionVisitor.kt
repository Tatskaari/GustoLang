package tatskaari.parsing.typechecking

import tatskaari.GustoType
import tatskaari.GustoType.*
import tatskaari.parsing.*

class TypeCheckerExpressionVisitor(val env: TypeEnv, private val typeErrors: Errors) : IExpressionVisitor<TypedExpression> {
  override fun visit(match: Expression.Match): TypedExpression {
    val expression = match.expression.accept(this)
    val matchBranches = match.matchBranches.map {
      val statementVisitor = TypeCheckerStatementVisitor(env, typeErrors, UnknownType)
      statementVisitor.checkPattern(it.pattern, expression.gustoType, expression.expression)
      val statement = it.statement.accept(statementVisitor)
      TypedMatchBranch(it.pattern, statement)
    }

    val returnType = matchBranches.first().statement.returnType ?: GustoType.PrimitiveType.Unit
    matchBranches.forEach {
      val branchReturnType = it.statement.returnType ?: PrimitiveType.Unit
      if(!TypeComparator.compareTypes(returnType, branchReturnType, HashMap())){
        typeErrors.addTypeMissmatch(it.statement.stmt, returnType, branchReturnType)
      }
    }
    val elseBranch = if (match.elseBranch is ElseMatchBranch.ElseBranch){
      val statementVisitor = TypeCheckerStatementVisitor(env, typeErrors, returnType)
      TypedElseBranch.ElseBranch(match.elseBranch.statement.accept(statementVisitor))
    } else {
      TypedElseBranch.NoElseBranch

    }

    return TypedExpression.Match(match, matchBranches, expression, elseBranch, returnType)
  }

  override fun visit(tuple: Expression.Tuple): TypedExpression {
    val type = TupleType(tuple.params.map { it.accept(this).gustoType })
    return TypedExpression.Tuple(tuple, type)
  }

  override fun visit(constructorCall: Expression.ConstructorCall): TypedExpression {
    if (!env.types.containsKey(constructorCall.name)) {
      typeErrors.add(constructorCall, "Constructor not defined.")
      return TypedExpression.ConstructorCall(constructorCall, null, VariantMember(constructorCall.name, UnknownType))
    }
    val type = env.types[constructorCall.name]!! as? GustoType.VariantMember ?:
      throw RuntimeException("Non-constructor called as a constructor")

    return if (constructorCall.expr != null){
      val expr = constructorCall.expr.accept(this)
      if(!TypeComparator.compareTypes(type.type, expr.gustoType, HashMap())) {
        typeErrors.addTypeMissmatch(expr.expression, type.type, expr.gustoType)
      }
      TypedExpression.ConstructorCall(constructorCall, expr, type)
    } else {
      if (type.type != PrimitiveType.Unit){
        typeErrors.addTypeMissmatch(constructorCall, type.type, PrimitiveType.Unit)
      }
      TypedExpression.ConstructorCall(constructorCall, null, type)
    }

  }

  override fun visit(intLiteral: Expression.IntLiteral): TypedExpression {
    return TypedExpression.IntLiteral(intLiteral)
  }

  override fun visit(numLiteral: Expression.NumLiteral): TypedExpression {
    return TypedExpression.NumLiteral(numLiteral)
  }

  override fun visit(booleanLiteral: Expression.BooleanLiteral): TypedExpression {
    return TypedExpression.BooleanLiteral(booleanLiteral)
  }

  override fun visit(textLiteral: Expression.TextLiteral): TypedExpression {
    return TypedExpression.TextLiteral(textLiteral)
  }

  override fun visit(identifier: Expression.Identifier): TypedExpression {
    return if (env.containsKey(identifier.name)){
      TypedExpression.Identifier(identifier, env.getValue(identifier.name))
    } else {
      typeErrors.add(identifier, "Identifier ${identifier.name} hasn't been declared yet")
      TypedExpression.Identifier(identifier, UnknownType)
    }
  }

  override fun visit(binaryOperator: Expression.BinaryOperator): TypedExpression {
    val lhs = binaryOperator.lhs.accept(this)
    val rhs = binaryOperator.rhs.accept(this)

    val lhsType = lhs.gustoType
    val rhsType = rhs.gustoType

    return when(binaryOperator.operator) {
      BinaryOperators.Add, BinaryOperators.Mul, BinaryOperators.Sub, BinaryOperators.Div -> {
        if ((lhsType == PrimitiveType.Text || rhsType == PrimitiveType.Text) && binaryOperator.operator == BinaryOperators.Add) {
          TypedExpression.Concatenation(binaryOperator, lhs, rhs)
        } else if (lhsType == PrimitiveType.Integer && rhsType == PrimitiveType.Integer) {
          TypedExpression.IntArithmeticOperation(binaryOperator, ArithmeticOperator.valueOf(binaryOperator.operator.name), lhs, rhs)
        } else if ((lhsType == PrimitiveType.Number || lhsType == PrimitiveType.Integer) && (rhsType == PrimitiveType.Number || rhsType == PrimitiveType.Integer)) {
          TypedExpression.NumArithmeticOperation(binaryOperator, ArithmeticOperator.valueOf(binaryOperator.operator.name), lhs, rhs)
        } else {
          typeErrors.addBinaryOperatorTypeError(binaryOperator, binaryOperator.operator, lhsType, rhsType)
          TypedExpression.NumArithmeticOperation(binaryOperator, ArithmeticOperator.valueOf(binaryOperator.operator.name), lhs, rhs)
        }
      }
      BinaryOperators.And, BinaryOperators.Or -> {
        if (lhsType == PrimitiveType.Boolean && rhsType == PrimitiveType.Boolean) {
          TypedExpression.BooleanLogicalOperation(binaryOperator, BooleanLogicalOperator.valueOf(binaryOperator.operator.name), lhs, rhs)
        } else {
          typeErrors.addBinaryOperatorTypeError(binaryOperator, binaryOperator.operator, lhsType, rhsType)
          TypedExpression.BooleanLogicalOperation(binaryOperator, BooleanLogicalOperator.valueOf(binaryOperator.operator.name), lhs, rhs)
        }
      }
      BinaryOperators.Equality -> TypedExpression.Equals(binaryOperator, lhs, rhs)
      BinaryOperators.NotEquality -> TypedExpression.NotEquals(binaryOperator, lhs, rhs)
      BinaryOperators.GreaterThan, BinaryOperators.GreaterThanEq, BinaryOperators.LessThan, BinaryOperators.LessThanEq -> {
        if ((lhsType == PrimitiveType.Number || lhsType == PrimitiveType.Integer) && (rhsType == PrimitiveType.Number || rhsType == PrimitiveType.Integer)) {
          if (lhs.gustoType == PrimitiveType.Number || rhs.gustoType == PrimitiveType.Number){
            TypedExpression.NumLogicalOperation(binaryOperator, NumericLogicalOperator.valueOf(binaryOperator.operator.name), lhs, rhs)
          } else {
            TypedExpression.IntLogicalOperation(binaryOperator, NumericLogicalOperator.valueOf(binaryOperator.operator.name), lhs, rhs)
          }
        } else {
          typeErrors.addBinaryOperatorTypeError(binaryOperator, binaryOperator.operator, lhsType, rhsType)
          TypedExpression.NumLogicalOperation(binaryOperator, NumericLogicalOperator.valueOf(binaryOperator.operator.name), lhs, rhs)
        }
      }
    }
  }

  override fun visit(unaryOperator: Expression.UnaryOperator): TypedExpression {
    val expressionType = unaryOperator.expression.accept(this)

    when(unaryOperator.operator){
      UnaryOperators.Negative -> {
        return when {
          expressionType.gustoType == PrimitiveType.Integer -> TypedExpression.NegateInt(unaryOperator, expressionType)
          expressionType.gustoType == PrimitiveType.Number -> TypedExpression.NegateNum(unaryOperator, expressionType)
          else -> {
            typeErrors.addUnaryOperatorTypeError(unaryOperator, unaryOperator.operator, expressionType.gustoType)
            TypedExpression.NegateNum(unaryOperator, expressionType)
          }
        }
      }
      UnaryOperators.Not -> {
        if (expressionType.gustoType != PrimitiveType.Boolean) {
          typeErrors.addUnaryOperatorTypeError(unaryOperator, unaryOperator.operator, expressionType.gustoType)
        }
        return TypedExpression.Not(unaryOperator, expressionType)
      }
    }
  }


  override fun visit(functionCall: Expression.FunctionCall): TypedExpression {
    val functionExpr = functionCall.functionExpression.accept(this)
    val functionType = functionExpr.gustoType
    val params = ArrayList<TypedExpression>()

    val genericTypes = HashMap<GenericType, GustoType>()

    return if (functionType is FunctionType){
      functionCall.params.zip(functionType.params).forEach { (paramExpr, type) ->
        val typedExpr = paramExpr.accept(this)
        params.add(typedExpr)
        if (!TypeComparator.compareTypes(type, typedExpr.gustoType, genericTypes)){
          typeErrors.addTypeMissmatch(functionCall, type, typedExpr.gustoType)
        }
      }

      TypedExpression.FunctionCall(functionCall, functionExpr, params, TypeComparator.expandFunctionType(functionType, genericTypes))
    } else {
      typeErrors.add(functionCall, "Unexpected type for target of a function call. Expected function, found $functionType")
      TypedExpression.FunctionCall(functionCall, functionExpr, params, FunctionType(listOf(), UnknownType))
    }
  }

  override fun visit(listAccess: Expression.ListAccess): TypedExpression {
    val listExpr = listAccess.listExpression.accept(this)
    val indexExpr = listAccess.indexExpression.accept(this)
    val listType = listExpr.gustoType


    return if ((listType is ListType && listType.type != UnknownType)){
      TypedExpression.ListAccess(listAccess, listType.type, listExpr, indexExpr)
    } else {
      // TODO improve this error message
      typeErrors.add(listAccess, "Expected list, found $listType")
      TypedExpression.ListAccess(listAccess, UnknownType, listExpr, indexExpr)
    }
  }

  override fun visit(listDeclaration: Expression.ListDeclaration): TypedExpression {
    return if (listDeclaration.items.isEmpty()){
      TypedExpression.ListDeclaration(listDeclaration, ListType(UnknownType), listOf())
    } else {
      val typedExpressions = ArrayList<TypedExpression>()
      val typedExpression = listDeclaration.items[0].accept(this)
      listDeclaration.items.forEach {
        val expressionType = it.accept(this)
        typedExpressions.add(expressionType)
        if (expressionType.gustoType != typedExpression.gustoType){
          typeErrors.addTypeMissmatch(listDeclaration, typedExpression.gustoType, expressionType.gustoType)
        }
      }
      TypedExpression.ListDeclaration(listDeclaration, ListType(typedExpression.gustoType), typedExpressions)
    }
  }

  override fun visit(function: Expression.Function): TypedExpression {
    val functionEnv = TypeEnv(env)
    val paramTypes = function.params.map {
      function.paramTypes.getValue(it)
    }
    //TODO pass in type definitions
    val params = function.paramTypes.map {
      (key, value) ->
        Pair(key.name, value.toGustoType(HashMap()))
    }
    functionEnv.putAll(params)
    val functionType = FunctionType(paramTypes.map { it.toGustoType(HashMap()) }, function.returnType.toGustoType(HashMap()))
    val body = function.body.accept(TypeCheckerStatementVisitor(functionEnv, typeErrors, functionType.returnType)) as TypedStatement.CodeBlock

    if (body.body.isEmpty() && functionType.returnType != PrimitiveType.Unit){
      typeErrors.add(function, "Missing return")
    }

    ReturnTypeChecker(typeErrors).codeblock(body, functionType.returnType != PrimitiveType.Unit)

    return TypedExpression.Function(function, body, functionType)
  }

}