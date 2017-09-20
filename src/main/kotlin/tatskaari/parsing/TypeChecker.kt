package tatskaari.parsing

import tatskaari.*
import tatskaari.tokenising.Token

class TypeChecker {
  sealed class TypedExpression(val gustoType: GustoType) {
    class NumLiteral(val expr: Expression.NumLiteral): TypedExpression(PrimitiveType.Number)
    class IntLiteral(val expr: Expression.IntLiteral): TypedExpression(PrimitiveType.Integer)
    class TextLiteral(val expr: Expression.TextLiteral): TypedExpression(PrimitiveType.Text)
    class BooleanLiteral(val expr: Expression.BooleanLiteral): TypedExpression(PrimitiveType.Boolean)
    class Identifier(val expr: Expression.Identifier, type: GustoType): TypedExpression(type)
    class UnaryOperator(val expr: Expression.UnaryOperator, val rhs: TypedExpression, type: GustoType): TypedExpression(type)
    class BinaryOperator(val expr: Expression.BinaryOperator, val lhs: TypedExpression, val rhs: TypedExpression, type: GustoType): TypedExpression(type)
    class FunctionCall(val expr: Expression.FunctionCall, val functionExpression: TypedExpression, val paramExprs: List<TypedExpression>, type: GustoType): TypedExpression(type)
    class ListAccess(val expr: Expression.ListAccess, type: GustoType, val listExpression: TypedExpression, val indexExpr: TypedExpression): TypedExpression(type)
    class ListDeclaration(val expr: Expression.ListDeclaration, type: GustoType, val listItemExpr: List<TypedExpression>) : TypedExpression(type)
    //TODO include the type of the body
    class Function(val expr: Expression.Function, type: GustoType): TypedExpression(type)
  }

  sealed class TypedStatement(val returnType: GustoType){
    class Assignment(val statement: Statement.Assignment, expression: TypedExpression): TypedStatement(PrimitiveType.Unit)
    class ValDeclaration(val statement: Statement.ValDeclaration, expression: TypedExpression): TypedStatement(PrimitiveType.Unit)
    class While(val statement: Statement.While, val body: TypedStatement, val condition: TypedExpression): TypedStatement(body.returnType)
    class CodeBlock(val statement: Statement.CodeBlock, val body: List<TypedStatement>, returnType: GustoType): TypedStatement(returnType)
    class If(val statement: Statement.If, val body: List<TypedStatement>, val condition: TypedExpression, returnType: GustoType): TypedStatement(returnType)
    class IfElse(val statement: Statement.IfElse, val trueBody: List<TypedStatement>, falseBody:List<TypedStatement>, val condition: TypedExpression, returnType: GustoType): TypedStatement(returnType)
    class Return(val statement: Statement.Return, val expression: TypedExpression): TypedStatement(expression.gustoType)
    class Output(val statement: Statement.Output, val expression: TypedExpression): TypedStatement(PrimitiveType.Unit)
    class Input(val statement: Statement.Input): TypedStatement(PrimitiveType.Unit)
    class FunctionDeclaration(val statement: Statement.FunctionDeclaration, val body: List<TypedStatement>, val functionType: FunctionType): TypedStatement(PrimitiveType.Unit)
    class ListAssignment(val statement: Statement.ListAssignment, val indexExpression: TypedExpression, val listExpression: TypedExpression): TypedStatement(PrimitiveType.Unit)
  }

  open class TypeCheckerException(reason: String, startToken: Token, endToken: Token) {
    val message: String = reason + " at " + startToken.lineNumber + ":" + startToken.columnNumber
  }

  class TypeMismatch : TypeCheckerException {
    constructor(expectedType: GustoType?, actualType: GustoType?, startToken: Token, endToken: Token) : super("Unexpected type $actualType, expected $expectedType", startToken, endToken)
    constructor(expectedTypes: List<GustoType>, actualType: GustoType, startToken: Token, endToken: Token) : super("Unexpected type $actualType, expected one of $expectedTypes", startToken, endToken)
    constructor(operator: BinaryOperators, lhsType: GustoType, rhsType: GustoType, startToken: Token, endToken: Token) : super("Operator $operator cannot be applied between $lhsType and $rhsType", startToken, endToken)
  }

  class FunctionCalledOnUncalleble(calledType: GustoType, startToken: Token, endToken: Token): TypeCheckerException("$calledType cannot be called as a function", startToken, endToken)
  class UndeclaredIdentifier(identifier: String, startToken: Token, endToken: Token): TypeCheckerException("$identifier has not been defined yet", startToken, endToken)

  var typeMismatches = ArrayList<TypeCheckerException>()

  private fun getExpressionType(expression: Expression, env: HashMap<String, GustoType>): TypedExpression {
    return when(expression) {
      is Expression.NumLiteral -> TypedExpression.NumLiteral(expression)
      is Expression.IntLiteral -> TypedExpression.IntLiteral(expression)
      is Expression.TextLiteral -> TypedExpression.TextLiteral(expression)
      is Expression.BooleanLiteral -> TypedExpression.BooleanLiteral(expression)
      is Expression.Identifier -> {
        if (!env.containsKey(expression.name)){
          typeMismatches.add(UndeclaredIdentifier(expression.name, expression.startToken, expression.endToken))
          TypedExpression.Identifier(expression, UnknownType)
        } else {
          TypedExpression.Identifier(expression, env.getValue(expression.name))
        }
      }
      is Expression.UnaryOperator -> getUnaryOperatorType(expression, env)
      is Expression.BinaryOperator -> getBinaryOperatorType(expression, env)
      is Expression.FunctionCall -> {
        val functionExpr = getExpressionType(expression.functionExpression, env)
        val functionType = functionExpr.gustoType
        val params = ArrayList<TypedExpression>()
        if (functionType is FunctionType){
          expression.params.zip(functionType.params).forEach { (expr, type) ->
            val exprType = getExpressionType(expr, env)
            params.add(exprType)
            if (exprType.gustoType != type){
              typeMismatches.add(TypeMismatch(type, exprType.gustoType, expr.startToken, expr.endToken))
            }
          }
          TypedExpression.FunctionCall(expression, functionExpr, params, functionType.returnType)
        } else {
          typeMismatches.add(FunctionCalledOnUncalleble(functionType, expression.startToken, expression.endToken))
          TypedExpression.FunctionCall(expression, functionExpr, params, UnknownType)
        }
      }
      is Expression.ListAccess -> {
        val listExpr = getExpressionType(expression.listExpression, env)
        val indexExpr = getExpressionType(expression.indexExpression, env)
        val listType = listExpr.gustoType
        if (!(listType is ListType && listType.type != null)){
          typeMismatches.add(TypeMismatch(ListType(null), listExpr.gustoType, expression.startToken, expression.endToken))
          TypedExpression.ListAccess(expression, UnknownType, listExpr, indexExpr)
        } else {
          TypedExpression.ListAccess(expression, listType.type, listExpr, indexExpr)
        }

      }
      is Expression.ListDeclaration -> getListType(expression, env)
      is Expression.Function ->{
        val paramTypes = expression.params.map { expression.paramTypes.getValue(it) }
        TypedExpression.Function(expression, FunctionType(paramTypes, expression.returnType))
      }
    }
  }

  private fun getListType(listDeclaration: Expression.ListDeclaration, env: HashMap<String, GustoType>): TypedExpression.ListDeclaration {
    return if (listDeclaration.items.isEmpty()){
      TypedExpression.ListDeclaration(listDeclaration, ListType(null), listOf())
    } else {
      val typedExpressions = ArrayList<TypedExpression>()
      val typedExpression = getExpressionType(listDeclaration.items[0], env)
      listDeclaration.items.forEach {
        val expressionType = getExpressionType(it, env)
        typedExpressions.add(expressionType)
        if (expressionType.gustoType != typedExpression.gustoType){
          typeMismatches.add(TypeMismatch(typedExpression.gustoType, expressionType.gustoType, listDeclaration.startToken, listDeclaration.endToken))
        }
      }
      TypedExpression.ListDeclaration(listDeclaration, ListType(typedExpression.gustoType), typedExpressions)
    }
  }

  private fun getUnaryOperatorType(unaryOp: Expression.UnaryOperator, env: HashMap<String, GustoType>): TypedExpression.UnaryOperator {
    val expressionType = getExpressionType(unaryOp.expression, env)

    when(unaryOp.operator){
      UnaryOperators.Negative -> {
        if (!(expressionType.gustoType == PrimitiveType.Integer || expressionType.gustoType == PrimitiveType.Number)){
          typeMismatches.add(TypeMismatch(listOf(PrimitiveType.Number, PrimitiveType.Integer), expressionType.gustoType, unaryOp.startToken, unaryOp.endToken))
          return TypedExpression.UnaryOperator(unaryOp, expressionType, UnknownType)
        }
      }
      UnaryOperators.Not -> {
        if (expressionType.gustoType != PrimitiveType.Boolean) {
          typeMismatches.add(TypeMismatch(PrimitiveType.Boolean, expressionType.gustoType, unaryOp.startToken, unaryOp.endToken))
          return TypedExpression.UnaryOperator(unaryOp, expressionType, UnknownType)
        }
      }
    }

    return TypedExpression.UnaryOperator(unaryOp, expressionType, expressionType.gustoType)
  }

  private fun getBinaryOperatorType(binOp: Expression.BinaryOperator, env: HashMap<String, GustoType>): TypedExpression.BinaryOperator {
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
          typeMismatches.add(TypeMismatch(binOp.operator, lhsType, rhsType, binOp.startToken, binOp.endToken))
          UnknownType
        }
      }
      BinaryOperators.And, BinaryOperators.Or -> {
        if (lhsType == PrimitiveType.Boolean && rhsType == PrimitiveType.Boolean){
          PrimitiveType.Boolean
        } else {
          typeMismatches.add(TypeMismatch(binOp.operator, lhsType, rhsType, binOp.startToken, binOp.endToken))
          UnknownType
        }
      }
      BinaryOperators.Equality, BinaryOperators.NotEquality -> PrimitiveType.Boolean
      BinaryOperators.GreaterThan, BinaryOperators.GreaterThanEq, BinaryOperators.LessThan, BinaryOperators.LessThanEq -> {
        if ((lhsType == PrimitiveType.Number || lhsType == PrimitiveType.Integer) && (rhsType == PrimitiveType.Number || rhsType == PrimitiveType.Integer)) {
          PrimitiveType.Boolean
        } else {
          typeMismatches.add(TypeMismatch(binOp.operator, lhsType, rhsType, binOp.startToken, binOp.endToken))
          UnknownType

        }
      }
    }

    return TypedExpression.BinaryOperator(binOp, lhs, rhs, type)
  }
  fun checkStatementListTypes(statements: List<Statement>, env: HashMap<String, GustoType>): Pair<List<TypedStatement>, GustoType>{
    var type: GustoType = PrimitiveType.Unit
    val typedStatements = ArrayList<TypedStatement>()
    statements.forEach {
      val typedStatement = checkStatmentType(it, env)
      typedStatements.add(typedStatement)
      if (type == PrimitiveType.Unit) {
        type = typedStatement.returnType
      } else if(typedStatement.returnType != PrimitiveType.Unit && typedStatement.returnType != type){
        typeMismatches.add(TypeMismatch(typedStatement.returnType, type, it.startTok, it.endTok))
      }
    }
    return Pair(typedStatements, type)
  }

  private fun checkStatmentType(statement: Statement, env: HashMap<String, GustoType>): TypedStatement{
      return when (statement) {
        is Statement.Assignment -> {
          val expression = getExpressionType(statement.expression, env)
          val expectedVal = env.getValue(statement.identifier.name)
          if (expression.gustoType != expectedVal) {
            typeMismatches.add(TypeMismatch(expectedVal, expression.gustoType, statement.startToken, statement.endToken))
          }
          TypedStatement.Assignment(statement, expression)
        }
        is Statement.ValDeclaration -> {
          val expression = getExpressionType(statement.expression, env)
          val expressionType = expression.gustoType
          if (expressionType != statement.type){
            typeMismatches.add(TypeMismatch(statement.type, expressionType, statement.startToken, statement.endToken))
          }
          env.put(statement.identifier.name, statement.type)
          TypedStatement.ValDeclaration(statement, expression)
        }
        is Statement.While -> {
          val typedConditionExpr = getExpressionType(statement.condition, env)
          if (typedConditionExpr.gustoType != PrimitiveType.Boolean) {
            typeMismatches.add(TypeMismatch(PrimitiveType.Boolean, typedConditionExpr.gustoType, statement.startToken, statement.endToken))
          }
          val bodyStatement = checkStatmentType(statement.body, env)
          TypedStatement.While(statement, bodyStatement, typedConditionExpr)
        }
        is Statement.CodeBlock -> {
          val (body, returnType) = checkStatementListTypes(statement.statementList, HashMap(env))
          TypedStatement.CodeBlock(statement, body, returnType)
        }
        is Statement.If -> {
          val typedConditionExpr = getExpressionType(statement.condition, env)
          if (typedConditionExpr.gustoType != PrimitiveType.Boolean) {
            typeMismatches.add(TypeMismatch(PrimitiveType.Boolean, typedConditionExpr.gustoType, statement.startToken, statement.endToken))
          }
          val (typedBody, returnType) = checkStatementListTypes(statement.body, env)
          TypedStatement.If(statement, typedBody, typedConditionExpr, returnType)
        }
        is Statement.IfElse -> {
          val typedCondition = getExpressionType(statement.condition, env)
          val conditionType = typedCondition.gustoType
          if (conditionType != PrimitiveType.Boolean) {
            typeMismatches.add(TypeMismatch(PrimitiveType.Boolean, conditionType, statement.startToken, statement.endToken))
          }
          val (typedIfBody, ifReturnType) = checkStatementListTypes(statement.ifBody, env)
          val (typedElseBody, elseReturnType) = checkStatementListTypes(statement.elseBody, env)
          if (ifReturnType != elseReturnType) {
            typeMismatches.add(TypeMismatch(ifReturnType, elseReturnType, statement.startToken, statement.endToken))
          }
          TypedStatement.IfElse(statement, typedIfBody, typedElseBody, typedCondition, ifReturnType)
        }
        is Statement.Return ->  TypedStatement.Return(statement, getExpressionType(statement.expression, env))
        is Statement.Output -> {
          TypedStatement.Output(statement, getExpressionType(statement.expression, env))
        }
        is Statement.Input -> {
          env.put(statement.identifier.name, PrimitiveType.Text)
          return TypedStatement.Input(statement)
        }
        is Statement.FunctionDeclaration -> {
          val functionEnv = HashMap(env)
          functionEnv.putAll(statement.function.paramTypes.mapKeys { it.key.name })

          val functionType = FunctionType(statement.function.params.map { statement.function.paramTypes.getValue(it) }, statement.function.returnType)

          functionEnv.put(statement.identifier.name, functionType)
          val (bodyStatements, bodyReturnType) = checkStatementListTypes(statement.function.body.statementList, functionEnv)

          env.put(statement.identifier.name, functionType)

          if(bodyReturnType != functionType.returnType){
            // if the return type of the body was null, this means that there was already a type missmatch in one of the
            // expressions so we shouldn't add this as a type mismatch
            val functionName = statement.identifier.name
            val returnType = statement.function.returnType
            typeMismatches.add(TypeCheckerException("The return type of $functionName is $returnType however the body of the function returns $bodyReturnType", statement.startToken, statement.endToken))
          }
          TypedStatement.FunctionDeclaration(statement, bodyStatements, functionType)
        }
        is Statement.ListAssignment -> {
          val listType = env.getValue(statement.identifier.name)
          val listExpr = getExpressionType(statement.expression, env)
          val indexExpr = getExpressionType(statement.indexExpression, env)

          if (listType is ListType){
            val expressionType = listExpr.gustoType
            val indexType = indexExpr.gustoType
            if (expressionType != listType.type){
              typeMismatches.add(TypeMismatch(listType.type, expressionType, statement.startToken, statement.endToken))
            }
            if (indexType != PrimitiveType.Integer){
              typeMismatches.add(TypeMismatch(PrimitiveType.Number, indexType, statement.startToken, statement.endToken))
            }
          } else {
            typeMismatches.add(TypeMismatch(ListType(null), listType, statement.startToken, statement.endToken))
          }
          TypedStatement.ListAssignment(statement, indexExpr, listExpr)
        }
      }
  }
}