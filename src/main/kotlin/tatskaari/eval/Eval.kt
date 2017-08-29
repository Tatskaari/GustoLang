package tatskaari.eval

import tatskaari.eval.values.Value
import tatskaari.parsing.*


typealias Env = Map<String, Value>
typealias MutEnv = HashMap<String, Value>

class Eval(private val inputProvider: InputProvider, private val outputProvider: OutputProvider) {

  data class TypeMismatch(override val message: String) : RuntimeException(message)
  object CastException: Exception()
  data class UndefinedIdentifier(override val message: String) : RuntimeException("Undeclared identifier '$message'")
  data class VariableAlreadyDefined(val identifier: String) : RuntimeException("Identifier aready declared '$identifier'")
  object InvalidUserInput : RuntimeException("Please enter a number, some text or the value 'true' or 'false'")
  //TODO better error message
  object FunctionExitedWithoutReturn : RuntimeException("Function exited without return")


  fun eval(statements: List<Statement>, env: MutEnv) : Value?  {
    statements.forEach {
      val value = eval(it, env)
      if (value != null) {
        return value
      }
    }
    return null
  }

  fun eval(statement: Statement, env: MutEnv) : Value? {
    when (statement) {
      is Statement.CodeBlock -> return eval(statement.statementList, HashMap(env))
      is Statement.If -> return evalIf(statement.condition, statement.body, null, HashMap(env))
      is Statement.IfElse -> return evalIf(statement.condition, statement.ifBody, statement.elseBody, HashMap(env))
      is Statement.While -> return evalWhile(statement.condition, statement.body.statementList, HashMap(env))
      is Statement.ValDeclaration-> {
        val identifierName = statement.identifier.name

        if (env.containsKey(identifierName)){
          throw VariableAlreadyDefined(identifierName)
        } else {
          env[identifierName] = eval(statement.expression, env)
        }
        return null
      }
      is Statement.Function -> {
        val identifierName = statement.identifier.name
        if (env.containsKey(identifierName)){
          throw VariableAlreadyDefined(identifierName)
        } else {
          val functionVal = Value.FunctionVal(statement, HashMap(env))
          functionVal.env.put(identifierName, functionVal)
          env[identifierName] = functionVal
        }
        return null
      }
      is Statement.Assignment -> {
        val identifier = statement.identifier.name
        val value = eval(statement.expression, env)
        if (env.containsKey(identifier)) {
          val existingValue = env.getValue(identifier)
          if (existingValue::class != value::class) {
            throw TypeMismatch("$identifier was already set to $existingValue, new value was $value")
          }
          env.getValue(identifier).value = value.value
        } else {
          throw UndefinedIdentifier(identifier)
        }
        return null
      }
      is Statement.ListAssignment -> {
        val index = eval(statement.indexExpression, env).intVal()
        val value = eval(statement.expression, env)
        val identifier = statement.identifier.name

        if (env.containsKey(identifier)) {
          val list = env.getValue(identifier).listVal()
          list[index] = value.copyLiteralOrReferenceList()
        } else {
          throw UndefinedIdentifier(identifier)
        }

        return null
      }
      is Statement.Input -> {
        val identifier = statement.identifier.name
        val input = inputProvider.readLine()
        if (input == null || input.isEmpty()) {
          throw InvalidUserInput
        } else {
          val value: Value =
            when {
              input.toIntOrNull() != null -> Value.IntVal(input.toInt())
              input.toDoubleOrNull() != null -> Value.NumVal(input.toDouble())
              input.equals("true") -> Value.BoolVal(true)
              input.equals("false") -> Value.BoolVal(false)
              else -> Value.TextVal(input)
            }
          env.put(identifier, value)
        }
        return null
      }
      is Statement.Output -> {
        //TODO make this work with io redirection
        val value = eval(statement.expression, env)
        outputProvider.println(value.value.toString())
        return null
      }
      is Statement.Return -> {
        return eval(statement.expression, env)
      }
    }
  }

  private fun evalWhile(condition: Expression, body: List<Statement>, env: MutEnv) : Value? {
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
      is Expression.Identifier -> {
        val value = env[expression.name]
        if (value != null) {
          return value
        } else {
          throw UndefinedIdentifier(expression.name)
        }
      }
    }
  }

  private fun evalList(expression: Expression.ListDeclaration, env: Env): Value.ListVal {
    val list = HashMap<Int, Value>()
    expression.items.forEachIndexed { index, expr->
      list.put(index, eval(expr, env))
    }

    return Value.ListVal(list)
  }

  private fun evalListAccess(expression: Expression.ListAccess, env: Env): Value{
    val list = eval(expression.listExpression, env)
    val index = eval(expression.indexExpression, env).intVal()
    return list.listVal().getValue(index).copyLiteralOrReferenceList()
  }


  private fun callFunction(functionCall: Expression.FunctionCall, env: Env) : Value{
    val functionVal = getFunction(functionCall, env)
    val funEnv = getFunctionRunEnv(functionVal.functionVal(), functionCall, functionVal.env, env)

    return evalFunction(functionVal.functionVal().body.statementList, funEnv)
  }

  private fun getFunction(functionCall: Expression.FunctionCall, env: Env): Value.FunctionVal {

    val function = eval(functionCall.functionExpression, env)

    if (function !is Value.FunctionVal){
      throw TypeMismatch("$function is not callable")
    }

    return function
  }

  private fun getFunctionRunEnv(function : Statement.Function, functionCall: Expression.FunctionCall, functionDefEnv: Env, functionCallEnv: Env) : MutEnv {
    if (functionCall.params.size != function.params.size){
      throw TypeMismatch("Wrong number of arguments to call $function ")
    }

    val functionRunEnv = HashMap<String, Value>(functionDefEnv)

    functionCall.params.forEachIndexed({index, expression ->
      val paramVal : Value = eval(expression, functionCallEnv)
      functionRunEnv.put(function.params[index].name, paramVal)
    })

    return functionRunEnv
  }

  private fun evalFunction(body: List<Statement>, env: MutEnv) : Value {
    body.forEach{
      val value = eval(it, env)
      if (value != null){
        return value
      }
    }
    throw FunctionExitedWithoutReturn
  }

  private fun applyBinaryOperator(operatorExpression: Expression.BinaryOperator, env: Env): Value {
    val operation = operatorExpression.operator
    val lhsVal = eval(operatorExpression.lhs, env)
    val rhsVal = eval(operatorExpression.rhs, env)
    when (operation) {
      BinaryOperators.Add -> {
        if (lhsVal is Value.Addable && rhsVal is Value.Addable){
          return lhsVal.plus(rhsVal)
        } else {
          throw TypeMismatch("$operation cannot be applied to $lhsVal and $rhsVal")
        }
      }
      BinaryOperators.Sub -> {
        if (lhsVal is Value.Subtractable && rhsVal is Value.Subtractable){
          return lhsVal.minus(rhsVal)
        } else {
          throw TypeMismatch("$operation cannot be applied to $lhsVal and $rhsVal")
        }
      }
      BinaryOperators.Div -> {
        if (lhsVal is Value.Divisible && rhsVal is Value.Divisible){
          return lhsVal.div(rhsVal)
        } else {
          throw TypeMismatch("$operation cannot be applied to $lhsVal and $rhsVal")
        }
      }
      BinaryOperators.Mul -> {
        if (lhsVal is Value.Multiplicable && rhsVal is Value.Multiplicable){
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

  private fun evalIf(condition: Expression, ifBody: List<Statement>, elseBody: List<Statement>?, env: MutEnv) : Value? {
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

