package tatskaari.eval

import tatskaari.GustoType
import tatskaari.eval.values.*
import tatskaari.parsing.*



class Eval(private val inputProvider: InputProvider, private val outputProvider: OutputProvider) {

  data class TypeMismatch(override val message: String) : RuntimeException(message)
  object CastException: Exception()
  data class UndefinedIdentifier(override val message: String) : RuntimeException("Undeclared identifier '$message'")
  data class VariableAlreadyDefined(val identifier: String) : RuntimeException("Identifier already declared '$identifier'")
  object InvalidUserInput : RuntimeException("Please enter a number, some text or the value 'true' or 'false'")


  fun eval(statements: List<Statement>, env: Env) : Value?  {
    statements.forEach {
      val value = eval(it, env)
      if (value != null) {
        return value
      }
    }
    return null
  }

  fun eval(statement: Statement, env: Env) : Value? {
    when (statement) {
      is Statement.CodeBlock -> return eval(statement.statementList, Env(env))
      is Statement.If -> return evalIf(statement.condition, statement.body.statementList, null, Env(env))
      is Statement.IfElse -> return evalIf(statement.condition, statement.ifBody.statementList, statement.elseBody.statementList, Env(env))
      is Statement.While -> return evalWhile(statement.condition, statement.body.statementList, Env(env))
      is Statement.ValDeclaration-> {
        val identifierName = statement.identifier.name

        if (env.hasVariable(identifierName)){
          throw VariableAlreadyDefined(identifierName)
        } else {
          env[identifierName] = eval(statement.expression, env).copyLiteralOrReferenceList()
        }
        return null
      }
      is Statement.FunctionDeclaration -> {
        val identifierName = statement.identifier.name
        if (env.hasVariable(identifierName)){
          throw VariableAlreadyDefined(identifierName)
        } else {
          val functionVal = Value.FunctionVal(statement.function, Env(env))
          functionVal.env[identifierName] = functionVal
          env[identifierName] = functionVal
        }
        return null
      }
      is Statement.Assignment -> {
        val identifier = statement.identifier.name
        val value = eval(statement.expression, env).copyLiteralOrReferenceList()
        val existingValue = env[identifier]
        if (existingValue::class != value::class) {
          throw TypeMismatch("$identifier was already set to $existingValue, new value was $value")
        }
        env[identifier].value = value.value

        return null
      }
      is Statement.ListAssignment -> {
        val index = eval(statement.indexExpression, env).intVal()
        val value = eval(statement.expression, env)
        val identifier = statement.identifier.name

        val list = env[identifier].listVal()
        list[index] = value.copyLiteralOrReferenceList()


        return null
      }
      is Statement.Input -> {
        val identifier = statement.identifier.name
        val input = inputProvider.readLine()
        if (input == null || input.isEmpty()) {
          throw InvalidUserInput
        } else {
          val value: Value = Value.TextVal(input)
          env[identifier] = value
        }
        return null
      }
      is Statement.Output -> {
        val value = eval(statement.expression, env)
        outputProvider.println(value.value.toString())
        return null
      }
      is Statement.Return -> {
        return eval(statement.expression, env)
      }
      is Statement.ExpressionStatement -> {
        eval(statement.expression, env)
        return null
      }
      is Statement.TypeDeclaration -> {
        val members = statement.members.map { it.toGustoType(env.typeDefinitions) }
        env.typeDefinitions.putAll(members.associate { Pair(it.name, it) })
        env.typeDefinitions[statement.identifier.name] = GustoType.VariantType(statement.identifier.name, members)
        return null
      }
    }
  }

  private fun evalWhile(condition: Expression, body: List<Statement>, env: Env) : Value? {
    while(evalCondition(condition, env).boolVal()){
      val value : Value? = eval(body, env)
      if (value != null) {
        return value
      }
    }
    return null
  }

  private fun evalCondition(condition: Expression, env: Env) : Value.BoolVal {
    val value = eval(condition, env)
    if (value is Value.BoolVal) {
      return value
    } else {
      throw TypeMismatch("condition did not return boolean result '$condition'")
    }
  }

  fun eval(expression: Expression, env: Env): Value {
    when (expression) {
      is Expression.IntLiteral -> return Value.IntVal(expression.value)
      is Expression.NumLiteral -> return Value.NumVal(expression.value)
      is Expression.BooleanLiteral -> return Value.BoolVal(expression.value)
      is Expression.TextLiteral -> return Value.TextVal(expression.value)
      is Expression.BinaryOperator -> return applyBinaryOperator(expression, env)
      is Expression.UnaryOperator -> return applyUnaryOperator(expression, env)
      is Expression.FunctionCall -> return callFunction(expression, env)
      is Expression.ListDeclaration -> return evalList(expression, env)
      is Expression.ListAccess -> return evalListAccess(expression, env)
      is Expression.Function -> return Value.FunctionVal(expression, Env(env))
      is Expression.Identifier -> return env[expression.name]
      is Expression.ConstructorCall -> return Value.VariantVal(expression.name)
      is Expression.Tuple -> TODO("Handle tuples")
    }
  }

  private fun evalList(expression: Expression.ListDeclaration, env: Env): Value.ListVal {
    val list = HashMap<Int, Value>()
    expression.items.forEachIndexed { index, expr->
      list[index] = eval(expr, env)
    }

    return Value.ListVal(list)
  }

  private fun evalListAccess(expression: Expression.ListAccess, env: Env): Value{
    val list = eval(expression.listExpression, env)
    val index = eval(expression.indexExpression, env).intVal()
    return list.listVal().getValue(index).copyLiteralOrReferenceList()
  }


  private fun callFunction(functionCall: Expression.FunctionCall, env: Env) : Value{
    val functionVal = eval(functionCall.functionExpression, env)

    when (functionVal) {
      is Value.FunctionVal -> {
        val funEnv = getFunctionRunEnv(functionVal.functionVal(), functionCall, functionVal.env, env)
        return evalFunction(functionVal.functionVal().body.statementList, funEnv)
      }
      is Value.BifVal -> {
        val funEnv = Env()
        functionVal.bif.params
          .zip(functionCall.params)
          .forEach { (name, expr) ->
            funEnv[name] = eval(expr, env)
          }
        return functionVal.bif.function(funEnv)
      }
      else -> throw TypeMismatch("$functionVal is not callable")
    }

  }



  private fun getFunctionRunEnv(function : Expression.Function, functionCall: Expression.FunctionCall, functionDefEnv: Env, functionCallEnv: Env) : Env {
    if (functionCall.params.size != function.params.size){
      val paramTypes = function.paramTypes.values
        .toMutableList()
        .map { it.toGustoType(functionDefEnv.typeDefinitions) }
      val functionType = GustoType.FunctionType(paramTypes, function.returnType.toGustoType(functionDefEnv.typeDefinitions))
      throw TypeMismatch("Wrong number of arguments to call $functionType")
    }

    val functionRunEnv = Env(functionDefEnv)

    functionCall.params.forEachIndexed({index, expression ->
      val paramVal : Value = eval(expression, functionCallEnv)
      functionRunEnv[function.params[index].name] = paramVal
    })

    return functionRunEnv
  }

  private fun evalFunction(body: List<Statement>, env: Env) : Value {
    body.forEach{
      val value = eval(it, env)
      if (value != null){
        return value
      }
    }
    return Value.Unit
  }

  private fun applyBinaryOperator(operatorExpression: Expression.BinaryOperator, env: Env): Value {
    val operation = operatorExpression.operator
    val lhsVal = eval(operatorExpression.lhs, env)
    val rhsVal = eval(operatorExpression.rhs, env)
    when (operation) {
      BinaryOperators.Add -> {
        if (lhsVal is Addable && rhsVal is Addable){
          return lhsVal.plus(rhsVal)
        } else {
          throw TypeMismatch("$operation cannot be applied to $lhsVal and $rhsVal")
        }
      }
      BinaryOperators.Sub -> {
        if (lhsVal is Subtractable && rhsVal is Subtractable){
          return lhsVal.minus(rhsVal)
        } else {
          throw TypeMismatch("$operation cannot be applied to $lhsVal and $rhsVal")
        }
      }
      BinaryOperators.Div -> {
        if (lhsVal is Divisible && rhsVal is Divisible){
          return lhsVal.div(rhsVal)
        } else {
          throw TypeMismatch("$operation cannot be applied to $lhsVal and $rhsVal")
        }
      }
      BinaryOperators.Mul -> {
        if (lhsVal is Multiplicable && rhsVal is Multiplicable){
          return lhsVal.times(rhsVal)
        } else {
          throw TypeMismatch("$operation cannot be applied to $lhsVal and $rhsVal")
        }
      }
      BinaryOperators.Equality -> return Value.BoolVal(lhsVal == rhsVal)
      BinaryOperators.NotEquality -> return Value.BoolVal(lhsVal != rhsVal)
      BinaryOperators.LessThan -> return Value.BoolVal(lhsVal.numVal() < rhsVal.numVal())
      BinaryOperators.GreaterThan -> return Value.BoolVal(lhsVal.numVal() > rhsVal.numVal())
      BinaryOperators.LessThanEq -> return Value.BoolVal(lhsVal.numVal() <= rhsVal.numVal())
      BinaryOperators.GreaterThanEq -> return Value.BoolVal(lhsVal.numVal() >= rhsVal.numVal())
      BinaryOperators.And -> return Value.BoolVal(lhsVal.boolVal() && rhsVal.boolVal())
      BinaryOperators.Or -> return Value.BoolVal(lhsVal.boolVal() || rhsVal.boolVal())
    }

  }

  private fun applyUnaryOperator(operatorExpr: Expression.UnaryOperator, env: Env): Value{
    val result = eval(operatorExpr.expression, env)
    val operator = operatorExpr.operator
    return try {
      when(operator){
        UnaryOperators.Not -> Value.BoolVal(!result.boolVal())
        UnaryOperators.Negative -> Value.IntVal(-result.intVal())
      }
    } catch (e: CastException) {
      throw TypeMismatch("$operator cannot be applied to $result")
    }
  }

  private fun evalIf(condition: Expression, ifBody: List<Statement>, elseBody: List<Statement>?, env: Env) : Value? {
    val conditionResult = eval(condition, env)
    if (conditionResult is Value.BoolVal) {
      var value : Value? = null
      if (conditionResult.boolVal()) {
        value = eval(ifBody, env)
      } else if (elseBody != null) {
        value = eval(elseBody, env)
      }
      if (value != null){
        return value
      }
    } else {
      throw TypeMismatch("If statement condition returnType error expected bool got $condition")
    }
    return null
  }
}

operator fun Number.compareTo(numVal: Number): Int {
  if (this is Int){
    if (numVal is Int){
      return this.compareTo(numVal)
    } else if (numVal is Double){
      return this.compareTo(numVal)
    }
  } else if(this is Double){
    if (numVal is Int){
      return this.compareTo(numVal)
    } else if (numVal is Double){
      return this.compareTo(numVal)
    }
  }
  throw Eval.TypeMismatch("Cannot compare $this and $numVal")
}

