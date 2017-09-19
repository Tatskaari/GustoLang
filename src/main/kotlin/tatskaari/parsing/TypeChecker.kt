package tatskaari.parsing

import tatskaari.FunctionType
import tatskaari.GustoType
import tatskaari.ListType
import tatskaari.PrimitiveType
import tatskaari.tokenising.Token

class TypeChecker {
  sealed class TypedExpression(val expression: Expression, val gustoType: GustoType) {
    class NumLiteral(val expr: Expression.NumLiteral): TypedExpression(expr, PrimitiveType.Number)
    class IntLiteral(val expr: Expression.IntLiteral): TypedExpression(expr, PrimitiveType.Integer)
    class TextLiteral(val expr: Expression.TextLiteral): TypedExpression(expr, PrimitiveType.Text)
    class BooleanLiteral(val expr: Expression.BooleanLiteral): TypedExpression(expr, PrimitiveType.Boolean)
    class Identifier(val expr: Expression.Identifier, type: GustoType): TypedExpression(expr, type)
    class UnaryOperator(val expr: Expression.UnaryOperator, val rhs: TypedExpression, type: GustoType): TypedExpression(expr, type)
    class BinaryOperator(val expr: Expression.BinaryOperator, val lhs: TypedExpression, val rhs: TypedExpression, type: GustoType): TypedExpression(expr, type)
    class FunctionCall(val expr: Expression.FunctionCall, val functionExpression: TypedExpression, val paramExprs: List<TypedExpression>, type: GustoType): TypedExpression(expr, type)
    class ListAccess(val expr: Expression.ListAccess, type: GustoType, val listExpression: TypedExpression, val indexExpr: TypedExpression): TypedExpression(expr, type)
    class ListDeclaration(val expr: Expression.ListDeclaration, type: GustoType, val listItemExpr: List<TypedExpression>) : TypedExpression(expr, type)
    //TODO include the type of the body
    class Function(val expr: Expression.Function, type: GustoType): TypedExpression(expr, type)
  }

  open class TypeCheckerException(reason: String, startToken: Token, endToken: Token): Exception(reason + " at " + startToken.lineNumber + ":" + startToken.columnNumber)

  class TypeMismatch : TypeCheckerException {
    constructor(expectedType: GustoType?, actualType: GustoType?, startToken: Token, endToken: Token) : super("Unexpected type $actualType, expected $expectedType", startToken, endToken)
    constructor(expectedTypes: List<GustoType>, actualType: GustoType, startToken: Token, endToken: Token) : super("Unexpected type $actualType, expected one of $expectedTypes", startToken, endToken)
    constructor(operator: BinaryOperators, lhsType: GustoType, rhsType: GustoType, startToken: Token, endToken: Token) : super("Operator $operator cannot be applied between $lhsType and $rhsType", startToken, endToken)
  }

  class FunctionCalledOnUncalleble(calledType: GustoType, startToken: Token, endToken: Token): TypeCheckerException("$calledType cannot be called as a function", startToken, endToken)
  class UndeclaredIdentifier(identifier: String, startToken: Token, endToken: Token): TypeCheckerException("$identifier has not been defined yet", startToken, endToken)

  var typeMismatches = ArrayList<TypeCheckerException>()

  fun getExpressionType(expression: Expression, env: HashMap<String, GustoType>): TypedExpression {
    return when(expression) {
      is Expression.NumLiteral -> TypedExpression.NumLiteral(expression)
      is Expression.IntLiteral -> TypedExpression.IntLiteral(expression)
      is Expression.TextLiteral -> TypedExpression.TextLiteral(expression)
      is Expression.BooleanLiteral -> TypedExpression.BooleanLiteral(expression)
      is Expression.Identifier -> {
        if (env.containsKey(expression.name)){
          TypedExpression.Identifier(expression, env.getValue(expression.name))
        } else {
          throw UndeclaredIdentifier(expression.name, expression.startToken, expression.endToken)
        }
      }
      is Expression.UnaryOperator -> getUnaryOperatorType(expression, env)
      is Expression.BinaryOperator -> getBinaryOperatorType(expression, env)
      is Expression.FunctionCall -> {
        val functionExpr = getExpressionType(expression.functionExpression, env)
        val functionType = functionExpr.gustoType
        if (functionType is FunctionType){
          val params = ArrayList<TypedExpression>()
          expression.params.zip(functionType.params).forEach { (expr, type) ->
            val exprType = getExpressionType(expr, env)
            params.add(exprType)
            if (exprType.gustoType != type){
              throw TypeMismatch(type, exprType.gustoType, expr.startToken, expr.endToken)
            }
          }
          TypedExpression.FunctionCall(expression, functionExpr, params, functionType.returnType)
        } else {
          throw FunctionCalledOnUncalleble(functionType, expression.startToken, expression.endToken)
        }
      }
      is Expression.ListAccess -> {
        val listExpr = getExpressionType(expression.listExpression, env)
        val indexExpr = getExpressionType(expression.indexExpression, env)
        if (listExpr.gustoType is ListType && listExpr.gustoType.type != null){
          TypedExpression.ListAccess(expression, listExpr.gustoType.type, listExpr, indexExpr)
        } else {
          throw TypeMismatch(ListType(null), listExpr.gustoType, expression.startToken, expression.endToken)
        }
      }
      is Expression.ListDeclaration -> getListType(expression, env)
      is Expression.Function -> TypedExpression.Function(expression, FunctionType(expression.params.map { expression.paramTypes.getValue(it) }, expression.returnType))
    }
  }

  fun getListType(listDeclaration: Expression.ListDeclaration, env: HashMap<String, GustoType>): TypedExpression.ListDeclaration {
    return if (listDeclaration.items.isEmpty()){
      TypedExpression.ListDeclaration(listDeclaration, ListType(null), listOf())
    } else {
      val typedExpressions = ArrayList<TypedExpression>()
      val typedExpression = getExpressionType(listDeclaration.items[0], env)
      listDeclaration.items.forEach {
        val expressionType = getExpressionType(it, env)
        typedExpressions.add(expressionType)
        if (expressionType.gustoType != typedExpression.gustoType){
          throw TypeMismatch(typedExpression.gustoType, expressionType.gustoType, listDeclaration.startToken, listDeclaration.endToken)
        }
      }
      TypedExpression.ListDeclaration(listDeclaration, ListType(typedExpression.gustoType), typedExpressions)
    }
  }

  fun getUnaryOperatorType(unaryOp: Expression.UnaryOperator, env: HashMap<String, GustoType>): TypedExpression.UnaryOperator {
    val expressionType = getExpressionType(unaryOp.expression, env)

    when(unaryOp.operator){
      UnaryOperators.Negative -> {
        if (expressionType.gustoType == PrimitiveType.Integer || expressionType.gustoType == PrimitiveType.Number){
        } else {
          throw TypeMismatch(listOf(PrimitiveType.Number, PrimitiveType.Integer), expressionType.gustoType, unaryOp.startToken, unaryOp.endToken)
        }
      }
      UnaryOperators.Not -> {
        if (expressionType.gustoType == PrimitiveType.Boolean) {
        } else {
          throw TypeMismatch(PrimitiveType.Boolean, expressionType.gustoType, unaryOp.startToken, unaryOp.endToken)
        }
      }
    }

    return TypedExpression.UnaryOperator(unaryOp, expressionType, expressionType.gustoType);
  }

  fun getBinaryOperatorType(binOp: Expression.BinaryOperator, env: HashMap<String, GustoType>): TypedExpression.BinaryOperator {
    val lhs = getExpressionType(binOp.lhs, env)
    val rhs = getExpressionType(binOp.rhs, env)

    val lhsType = lhs.gustoType
    val rhsType = rhs.gustoType

    val type: GustoType = when(binOp.operator) {
      BinaryOperators.Add, BinaryOperators.Mul, BinaryOperators.Sub, BinaryOperators.Div -> {
        if (lhsType == PrimitiveType.Text || rhsType == PrimitiveType.Text) {
          PrimitiveType.Text
        } else if (lhsType == PrimitiveType.Number && (rhsType == PrimitiveType.Number || rhsType == PrimitiveType.Integer)){
          PrimitiveType.Number
        } else if (rhsType == PrimitiveType.Number && (lhsType == PrimitiveType.Number || lhsType == PrimitiveType.Integer)) {
          PrimitiveType.Number
        } else if (lhsType == PrimitiveType.Integer && rhsType == PrimitiveType.Integer) {
          PrimitiveType.Integer
        } else {
          throw TypeMismatch(binOp.operator, lhsType, rhsType, binOp.startToken, binOp.endToken)
        }
      }
      BinaryOperators.And, BinaryOperators.Or -> {
        if (lhsType == PrimitiveType.Boolean && rhsType == PrimitiveType.Boolean){
          PrimitiveType.Boolean
        } else {
          throw TypeMismatch(binOp.operator, lhsType, rhsType, binOp.startToken, binOp.endToken)
        }
      }
      BinaryOperators.Equality, BinaryOperators.NotEquality -> PrimitiveType.Boolean
      BinaryOperators.GreaterThan, BinaryOperators.GreaterThanEq, BinaryOperators.LessThan, BinaryOperators.LessThanEq -> {
        if ((lhsType == PrimitiveType.Number || lhsType == PrimitiveType.Integer) && (rhsType == PrimitiveType.Number || rhsType == PrimitiveType.Integer)) {
          PrimitiveType.Boolean
        } else {
          throw TypeMismatch(binOp.operator, lhsType, rhsType, binOp.startToken, binOp.endToken)
        }
      }
    }

    return TypedExpression.BinaryOperator(binOp, lhs, rhs, type)
  }
  fun checkStatementListTypes(statements: List<Statement>, env: HashMap<String, GustoType>): GustoType?{
    var type: GustoType? = PrimitiveType.Unit
    statements.forEach {
      val statementType = checkStatmentType(it, env)
      if (type == PrimitiveType.Unit) {
        type = statementType
      } else if(statementType != PrimitiveType.Unit && statementType != type){
        throw TypeMismatch(statementType, type, it.startTok, it.endTok)
      }
    }
    return type
  }

  fun checkStatmentType(statement: Statement, env: HashMap<String, GustoType>): GustoType?{
    try {
      when (statement) {
        is Statement.Assignment -> {
          val expressionVal = getExpressionType(statement.expression, env).gustoType
          val expectedVal = env.getValue(statement.identifier.name)
          if (expressionVal != expectedVal) {
            typeMismatches.add(TypeMismatch(expectedVal, expressionVal, statement.startToken, statement.endToken))
          }
          return PrimitiveType.Unit
        }
        is Statement.ValDeclaration -> {
          checkDeclarationType(statement, statement.type, env)
          return PrimitiveType.Unit
        }
        is Statement.While -> {
          val expressionType = getExpressionType(statement.condition, env).gustoType
          if (expressionType != PrimitiveType.Boolean) {
            typeMismatches.add(TypeMismatch(PrimitiveType.Boolean, expressionType, statement.startToken, statement.endToken))
          }
          return checkStatmentType(statement.body, env)
        }
        is Statement.CodeBlock -> return checkStatementListTypes(statement.statementList, HashMap(env))
        is Statement.If -> {
          val expressionType = getExpressionType(statement.condition, env).gustoType
          if (expressionType != PrimitiveType.Boolean) {
            typeMismatches.add(TypeMismatch(PrimitiveType.Boolean, expressionType, statement.startToken, statement.endToken))
          }
          return checkStatementListTypes(statement.body, env)
        }
        is Statement.IfElse -> {
          val expressionType = getExpressionType(statement.condition, env).gustoType
          if (expressionType != PrimitiveType.Boolean) {
            typeMismatches.add(TypeMismatch(PrimitiveType.Boolean, expressionType, statement.startToken, statement.endToken))
          }
          val ifType = checkStatementListTypes(statement.ifBody, env)
          val elseType = checkStatementListTypes(statement.elseBody, env)
          if (ifType == elseType) {
            return ifType
          } else {
            typeMismatches.add(TypeMismatch(ifType, elseType, statement.startToken, statement.endToken))
          }
        }
        is Statement.Return -> return getExpressionType(statement.expression, env).gustoType
        is Statement.Output -> {
          getExpressionType(statement.expression, env)
          return PrimitiveType.Unit
        }
        is Statement.Input -> {
          env.put(statement.identifier.name, PrimitiveType.Text)
          return PrimitiveType.Unit
        }
        is Statement.FunctionDeclaration -> {
          val functionEnv = HashMap(env)
          functionEnv.putAll(statement.function.paramTypes.mapKeys { it.key.name })

          val functionType = FunctionType(statement.function.params.map { statement.function.paramTypes.getValue(it) }, statement.function.returnType)

          functionEnv.put(statement.identifier.name, functionType)
          val bodyReturnType = checkStatementListTypes(statement.function.body.statementList, functionEnv)

          env.put(statement.identifier.name, functionType)

          if(bodyReturnType != null && bodyReturnType != functionType.returnType){
            // if the return type of the body was null, this means that there was already a type missmatch in one of the
            // expressions so we shouldn't add this as a type mismatch
            val functionName = statement.identifier.name
            val returnType = statement.function.returnType
            typeMismatches.add(TypeCheckerException("The return type of $functionName is $returnType however the body of the function returns $bodyReturnType", statement.startToken, statement.endToken))
          }
          return PrimitiveType.Unit
        }
        is Statement.ListAssignment -> {
          val listType = env.getValue(statement.identifier.name)
          if (listType is ListType){
            val expressionType = getExpressionType(statement.expression, env).gustoType
            val indexType = getExpressionType(statement.indexExpression, env).gustoType
            if (expressionType != listType.type){
              typeMismatches.add(TypeMismatch(listType.type, expressionType, statement.startToken, statement.endToken))
            }
            if (indexType != PrimitiveType.Integer){
              typeMismatches.add(TypeMismatch(PrimitiveType.Number, indexType, statement.startToken, statement.endToken))
            }
          } else {
            typeMismatches.add(TypeMismatch(ListType(null), listType, statement.startToken, statement.endToken))
          }
        }
      }
    } catch (typeMismatch: TypeCheckerException){
      typeMismatches.add(typeMismatch)
    }
    return null
  }

  fun checkDeclarationType(declaration: Statement.ValDeclaration, type: GustoType, env: HashMap<String, GustoType>){
    val expressionType = getExpressionType(declaration.expression, env).gustoType
    if (expressionType != type){
      typeMismatches.add(TypeMismatch(type, expressionType, declaration.startToken, declaration.endToken))
    }
    env.put(declaration.identifier.name, type)
  }
}