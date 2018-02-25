package tatskaari.parsing.typechecking

import tatskaari.GustoType
import tatskaari.GustoType.*
import tatskaari.parsing.*

class TypeCheckerExpressionVisitor(val env: TypeEnv, private val typeErrors: Errors) : IExpressionVisitor<TypedExpression> {
  override fun visitMatch(match: Expression.Match): TypedExpression {
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
        typeErrors.addTypeMismatch(it.statement.stmt, returnType, branchReturnType)
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

  override fun visitTuple(tuple: Expression.Tuple): TypedExpression {
    val type = TupleType(tuple.params.map { it.accept(this).gustoType })
    return TypedExpression.Tuple(tuple, type)
  }

  override fun visitConstructorCall(constructorCall: Expression.ConstructorCall): TypedExpression {
    if (!env.types.containsKey(constructorCall.name)) {
      typeErrors.add(constructorCall, "Constructor not defined.")
      return TypedExpression.ConstructorCall(constructorCall, null, VariantMember(constructorCall.name, UnknownType))
    }
    val type = env.types[constructorCall.name]!! as? GustoType.VariantMember ?:
      throw RuntimeException("Non-constructor called as a constructor")

    return if (constructorCall.expr != null){
      val expr = constructorCall.expr.accept(this)
      if(!TypeComparator.compareTypes(type.type, expr.gustoType, HashMap())) {
        typeErrors.addTypeMismatch(expr.expression, type.type, expr.gustoType)
      }
      TypedExpression.ConstructorCall(constructorCall, expr, type)
    } else {
      if (type.type != PrimitiveType.Unit){
        typeErrors.addTypeMismatch(constructorCall, type.type, PrimitiveType.Unit)
      }
      TypedExpression.ConstructorCall(constructorCall, null, type)
    }

  }

  override fun visitIntLiteral(intLiteral: Expression.IntLiteral): TypedExpression {
    return TypedExpression.IntLiteral(intLiteral)
  }

  override fun visitNumLiteral(numLiteral: Expression.NumLiteral): TypedExpression {
    return TypedExpression.NumLiteral(numLiteral)
  }

  override fun visitBoolLiteral(booleanLiteral: Expression.BooleanLiteral): TypedExpression {
    return TypedExpression.BooleanLiteral(booleanLiteral)
  }

  override fun visitTextLiteral(textLiteral: Expression.TextLiteral): TypedExpression {
    return TypedExpression.TextLiteral(textLiteral)
  }

  override fun visitIdentifier(identifier: Expression.Identifier): TypedExpression {
    return if (env.containsKey(identifier.name)){
      TypedExpression.Identifier(identifier, env.getValue(identifier.name))
    } else {
      typeErrors.add(identifier, "Identifier ${identifier.name} hasn't been declared yet")
      TypedExpression.Identifier(identifier, UnknownType)
    }
  }

  override fun visitBinaryOperation(binaryOperation: Expression.BinaryOperation): TypedExpression {
    val lhs = binaryOperation.lhs.accept(this)
    val rhs = binaryOperation.rhs.accept(this)

    val lhsType = lhs.gustoType
    val rhsType = rhs.gustoType

    return when(binaryOperation.operator) {
      BinaryOperators.Add, BinaryOperators.Mul, BinaryOperators.Sub, BinaryOperators.Div -> {
        if ((lhsType == PrimitiveType.Text || rhsType == PrimitiveType.Text) && binaryOperation.operator == BinaryOperators.Add) {
          TypedExpression.Concatenation(binaryOperation, lhs, rhs)
        } else if (lhsType == PrimitiveType.Integer && rhsType == PrimitiveType.Integer) {
          TypedExpression.IntArithmeticOperation(binaryOperation, ArithmeticOperator.valueOf(binaryOperation.operator.name), lhs, rhs)
        } else if ((lhsType == PrimitiveType.Number || lhsType == PrimitiveType.Integer) && (rhsType == PrimitiveType.Number || rhsType == PrimitiveType.Integer)) {
          TypedExpression.NumArithmeticOperation(binaryOperation, ArithmeticOperator.valueOf(binaryOperation.operator.name), lhs, rhs)
        } else {
          typeErrors.addBinaryOperatorTypeError(binaryOperation, binaryOperation.operator, lhsType, rhsType)
          TypedExpression.NumArithmeticOperation(binaryOperation, ArithmeticOperator.valueOf(binaryOperation.operator.name), lhs, rhs)
        }
      }
      BinaryOperators.And, BinaryOperators.Or -> {
        if (lhsType == PrimitiveType.Boolean && rhsType == PrimitiveType.Boolean) {
          TypedExpression.BooleanLogicalOperation(binaryOperation, BooleanLogicalOperator.valueOf(binaryOperation.operator.name), lhs, rhs)
        } else {
          typeErrors.addBinaryOperatorTypeError(binaryOperation, binaryOperation.operator, lhsType, rhsType)
          TypedExpression.BooleanLogicalOperation(binaryOperation, BooleanLogicalOperator.valueOf(binaryOperation.operator.name), lhs, rhs)
        }
      }
      BinaryOperators.Equality -> TypedExpression.Equals(binaryOperation, lhs, rhs)
      BinaryOperators.NotEquality -> TypedExpression.NotEquals(binaryOperation, lhs, rhs)
      BinaryOperators.GreaterThan, BinaryOperators.GreaterThanEq, BinaryOperators.LessThan, BinaryOperators.LessThanEq -> {
        if ((lhsType == PrimitiveType.Number || lhsType == PrimitiveType.Integer) && (rhsType == PrimitiveType.Number || rhsType == PrimitiveType.Integer)) {
          if (lhs.gustoType == PrimitiveType.Number || rhs.gustoType == PrimitiveType.Number){
            TypedExpression.NumLogicalOperation(binaryOperation, NumericLogicalOperator.valueOf(binaryOperation.operator.name), lhs, rhs)
          } else {
            TypedExpression.IntLogicalOperation(binaryOperation, NumericLogicalOperator.valueOf(binaryOperation.operator.name), lhs, rhs)
          }
        } else {
          typeErrors.addBinaryOperatorTypeError(binaryOperation, binaryOperation.operator, lhsType, rhsType)
          TypedExpression.NumLogicalOperation(binaryOperation, NumericLogicalOperator.valueOf(binaryOperation.operator.name), lhs, rhs)
        }
      }
    }
  }

  override fun visitUnaryOperator(unaryOperation: Expression.UnaryOperation): TypedExpression {
    val expressionType = unaryOperation.expression.accept(this)

    when(unaryOperation.operator){
      UnaryOperators.Negative -> {
        return when {
          expressionType.gustoType == PrimitiveType.Integer -> TypedExpression.NegateInt(unaryOperation, expressionType)
          expressionType.gustoType == PrimitiveType.Number -> TypedExpression.NegateNum(unaryOperation, expressionType)
          else -> {
            typeErrors.addUnaryOperatorTypeError(unaryOperation, unaryOperation.operator, expressionType.gustoType)
            TypedExpression.NegateNum(unaryOperation, expressionType)
          }
        }
      }
      UnaryOperators.Not -> {
        if (expressionType.gustoType != PrimitiveType.Boolean) {
          typeErrors.addUnaryOperatorTypeError(unaryOperation, unaryOperation.operator, expressionType.gustoType)
        }
        return TypedExpression.Not(unaryOperation, expressionType)
      }
    }
  }


  override fun visitFunctionCall(functionCall: Expression.FunctionCall): TypedExpression {
    val functionExpr = functionCall.functionExpression.accept(this)
    val functionType = functionExpr.gustoType
    val params = ArrayList<TypedExpression>()

    val genericTypes = HashMap<GenericType, GustoType>()

    return if (functionType is FunctionType){
      functionCall.params.zip(functionType.params).forEach { (paramExpr, type) ->
        val typedExpr = paramExpr.accept(this)
        params.add(typedExpr)
        if (!TypeComparator.compareTypes(type, typedExpr.gustoType, genericTypes)){
          typeErrors.addTypeMismatch(functionCall, type, typedExpr.gustoType)
        }
      }

      TypedExpression.FunctionCall(functionCall, functionExpr, params, TypeComparator.expandFunctionType(functionType, genericTypes))
    } else {
      typeErrors.add(functionCall, "Unexpected type for target of a function call. Expected function, found $functionType")
      TypedExpression.FunctionCall(functionCall, functionExpr, params, FunctionType(listOf(), UnknownType))
    }
  }

  override fun visitListAccess(listAccess: Expression.ListAccess): TypedExpression {
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

  override fun visitListDeclaration(listDeclaration: Expression.ListDeclaration): TypedExpression {
    return if (listDeclaration.items.isEmpty()){
      TypedExpression.ListDeclaration(listDeclaration, ListType(UnknownType), listOf())
    } else {
      val typedExpressions = ArrayList<TypedExpression>()
      val typedExpression = listDeclaration.items[0].accept(this)
      listDeclaration.items.forEach {
        val expressionType = it.accept(this)
        typedExpressions.add(expressionType)
        if (expressionType.gustoType != typedExpression.gustoType){
          typeErrors.addTypeMismatch(listDeclaration, typedExpression.gustoType, expressionType.gustoType)
        }
      }
      TypedExpression.ListDeclaration(listDeclaration, ListType(typedExpression.gustoType), typedExpressions)
    }
  }

  override fun visitFunction(function: Expression.Function): TypedExpression {
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
    val declaredFunctionType = FunctionType(paramTypes.map { it.toGustoType(HashMap()) }, function.returnType.toGustoType(HashMap()))
    val body = function.body.accept(TypeCheckerStatementVisitor(functionEnv, typeErrors, declaredFunctionType.returnType)) as TypedStatement.CodeBlock

    val inferredFunctionType = if(declaredFunctionType.returnType == GustoType.UnknownType){
      GustoType.FunctionType(declaredFunctionType.params, body.returnType ?: PrimitiveType.Unit)
    } else {
      declaredFunctionType
    }

    if (body.body.isEmpty() && inferredFunctionType.returnType != PrimitiveType.Unit){
      typeErrors.add(function, "Missing return")
    }

    ReturnTypeChecker(typeErrors).codeblock(body, inferredFunctionType.returnType != PrimitiveType.Unit)

    return TypedExpression.Function(function, body, inferredFunctionType)
  }

}