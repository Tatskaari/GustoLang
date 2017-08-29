package tatskaari.parsing

import tatskaari.FunctionType
import tatskaari.GustoType
import tatskaari.ListType
import tatskaari.PrimitiveType

class TypeChecker {
  open class TypeMismatch : Exception {
    constructor(expectedType: GustoType?, actualType: GustoType?) : super("Unexpected type $actualType, expected $expectedType")
    constructor(expectedTypes: List<GustoType>, actualType: GustoType) : super("Unexpected type $actualType, expected one of $expectedTypes")
    constructor(operator: BinaryOperators, lhsType: GustoType, rhsType: GustoType) : super("Operator $operator cannot be applied between $lhsType and $rhsType")
    constructor(reason: String) : super(reason)
  }

  class FunctionCalledOnUncalleble(calledType: GustoType): TypeMismatch("$calledType cannot be called as a function")

  var typeMismatches = ArrayList<TypeMismatch>()

  fun getExpressionType(expression: Expression, env: HashMap<String, GustoType>): GustoType {
    return when(expression) {
      is Expression.NumLiteral -> PrimitiveType.Number
      is Expression.IntLiteral -> PrimitiveType.Integer
      is Expression.TextLiteral -> PrimitiveType.Text
      is Expression.BooleanLiteral -> PrimitiveType.Boolean
      is Expression.Identifier -> env.getValue(expression.name)
      is Expression.UnaryOperator -> getUnaryOperatorType(expression, env)
      is Expression.BinaryOperator -> getBinaryOperatorType(expression, env)
      is Expression.FunctionCall -> {
        val functionType = getExpressionType(expression.functionExpression, env)
        if (functionType is FunctionType){
          functionType.returnType
        } else {
          throw FunctionCalledOnUncalleble(functionType)
        }
      }
      is Expression.ListAccess -> {
        val listType = getExpressionType(expression.listExpression, env)
        if (listType is ListType && listType.type != null){
          listType.type
        } else {
          throw TypeMismatch(ListType(null), listType)
        }
      }
      is Expression.ListDeclaration -> getListType(expression, env)
    }
  }

  fun getListType(listDeclaration: Expression.ListDeclaration, env: HashMap<String, GustoType>): GustoType {
    return if (listDeclaration.items.isEmpty()){
      ListType(null)
    } else {
      val type = getExpressionType(listDeclaration.items[0], env)
      listDeclaration.items.forEach {
        val expressionType = getExpressionType(it, env)
        if (expressionType != type){
          throw TypeMismatch(type, expressionType)
        }
      }
      ListType(type)
    }
  }

  fun getUnaryOperatorType(unaryOp: Expression.UnaryOperator, env: HashMap<String, GustoType>): GustoType {
    return when(unaryOp.operator){
      UnaryOperators.Negative -> {
        val expressionType = getExpressionType(unaryOp.expression, env)
        if (expressionType == PrimitiveType.Integer || expressionType == PrimitiveType.Number){
          expressionType
        } else {
          throw TypeMismatch(listOf(PrimitiveType.Number, PrimitiveType.Integer), expressionType)
        }
      }
      UnaryOperators.Not -> {
        val expressionType = getExpressionType(unaryOp.expression, env)
        if (expressionType == PrimitiveType.Boolean) {
          PrimitiveType.Boolean
        } else {
          throw TypeMismatch(PrimitiveType.Boolean, expressionType)
        }
      }

    }
  }

  fun getBinaryOperatorType(binOp: Expression.BinaryOperator, env: HashMap<String, GustoType>): GustoType {
    val lhsType = getExpressionType(binOp.lhs, env)
    val rhsType = getExpressionType(binOp.rhs, env)
    return when(binOp.operator) {
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
          throw TypeMismatch(binOp.operator, lhsType, rhsType)
        }
      }
      BinaryOperators.And, BinaryOperators.Or -> {
        if (lhsType == PrimitiveType.Boolean && rhsType == PrimitiveType.Boolean){
          PrimitiveType.Boolean
        } else {
          throw TypeMismatch(binOp.operator, lhsType, rhsType)
        }
      }
      BinaryOperators.Equality, BinaryOperators.NotEquality -> PrimitiveType.Boolean
      BinaryOperators.GreaterThan, BinaryOperators.GreaterThanEq, BinaryOperators.LessThan, BinaryOperators.LessThanEq -> {
        if ((lhsType == PrimitiveType.Number || lhsType == PrimitiveType.Integer) && (rhsType == PrimitiveType.Number || rhsType == PrimitiveType.Integer)) {
          PrimitiveType.Boolean
        } else {
          throw TypeMismatch(binOp.operator, lhsType, rhsType)
        }
      }
    }
  }
  fun checkStatementListTypes(statements: List<Statement>, env: HashMap<String, GustoType>): GustoType?{
    var type: GustoType? = PrimitiveType.Unit
    statements.forEach {
      val statementType = checkStatmentType(it, env)
      if (type == PrimitiveType.Unit) {
        type = statementType
      } else if(statementType != PrimitiveType.Unit && statementType != type){
        throw TypeMismatch(statementType, type)
      }
    }
    return type
  }

  fun checkStatmentType(statement: Statement, env: HashMap<String, GustoType>): GustoType?{
    try {
      when (statement) {
        is Statement.Assignment -> {
          val expressionVal = getExpressionType(statement.expression, env)
          val expectedVal = env.getValue(statement.identifier.name)
          if (expressionVal != expectedVal) {
            typeMismatches.add(TypeMismatch(expectedVal, expressionVal))
          }
          return PrimitiveType.Unit
        }
        is Statement.ValDeclaration -> {
          checkDeclarationType(statement, statement.type, env)
          return PrimitiveType.Unit
        }
        is Statement.While -> {
          val expressionType = getExpressionType(statement.condition, env)
          if (expressionType != PrimitiveType.Boolean) {
            typeMismatches.add(TypeMismatch(PrimitiveType.Boolean, expressionType))
          }
          return checkStatmentType(statement.body, env)
        }
        is Statement.CodeBlock -> return checkStatementListTypes(statement.statementList, HashMap(env))
        is Statement.If -> {
          val expressionType = getExpressionType(statement.condition, env)
          if (expressionType != PrimitiveType.Boolean) {
            typeMismatches.add(TypeMismatch(PrimitiveType.Boolean, expressionType))
          }
          return checkStatementListTypes(statement.body, env)
        }
        is Statement.IfElse -> {
          val expressionType = getExpressionType(statement.condition, env)
          if (expressionType != PrimitiveType.Boolean) {
            typeMismatches.add(TypeMismatch(PrimitiveType.Boolean, expressionType))
          }
          val ifType = checkStatementListTypes(statement.ifBody, env)
          val elseType = checkStatementListTypes(statement.elseBody, env)
          if (ifType == elseType) {
            return ifType
          } else {
            typeMismatches.add(TypeMismatch(ifType, elseType))
          }
        }
        is Statement.Return -> return getExpressionType(statement.expression, env)
        is Statement.Input, is Statement.Output -> return PrimitiveType.Unit
        is Statement.Function -> {
          val functionEnv = HashMap(env)
          functionEnv.putAll(statement.paramTypes.mapKeys { it.key.name })

          val functionType = FunctionType(statement.params.map { statement.paramTypes.getValue(it) }, statement.returnType)

          functionEnv.put(statement.identifier.name, functionType)
          val bodyReturnType = checkStatementListTypes(statement.body.statementList, functionEnv)
          if (bodyReturnType == statement.returnType) {
            env.put(statement.identifier.name, functionType)
          } else if(bodyReturnType != null){
            // if the return type of the body was null, this means that there was already a type missmatch in one of the
            // expressions so we shouldn't add this as a type missmatch
            typeMismatches.add(TypeMismatch(functionType.returnType, bodyReturnType))
          }
          return PrimitiveType.Unit
        }
        is Statement.ListAssignment -> {
          val listType = env.getValue(statement.identifier.name)
          if (listType is ListType){
            val expressionType = getExpressionType(statement.expression, env)
            val indexType = getExpressionType(statement.indexExpression, env)
            if (expressionType != listType.type){
              typeMismatches.add(TypeMismatch(listType.type, expressionType))
            }
            if (indexType != PrimitiveType.Integer){
              typeMismatches.add(TypeMismatch(PrimitiveType.Number, indexType))
            }
          } else {
            typeMismatches.add(TypeMismatch(ListType(null), listType))
          }
        }
      }
    } catch (typeMismatch: TypeMismatch){
      typeMismatches.add(typeMismatch)
    }
    return null
  }

  fun checkDeclarationType(declaration: Statement.ValDeclaration, type: GustoType, env: HashMap<String, GustoType>){
    val expressionType = getExpressionType(declaration.expression, env)
    if (expressionType != type){
      typeMismatches.add(TypeMismatch(type, expressionType))
    }
    env.put(declaration.identifier.name, type)
  }
}