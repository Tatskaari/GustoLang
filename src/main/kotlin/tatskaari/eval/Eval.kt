package tatskaari.eval

import tatskaari.parsing.Expression
import tatskaari.parsing.BinaryOperators
import tatskaari.parsing.Statement
import tatskaari.parsing.UnaryOperators


typealias Env = Map<String, Eval.Value>
typealias MutEnv = HashMap<String, Eval.Value>

class Eval {


  sealed class Value(var value: Any) {
    class NumVal(intVal: Int) : Value(intVal)
    class TextVal(textVal: String) : Value(textVal)
    class BoolVal(boolVal: Boolean) : Value(boolVal)
    class FunctionVal(functionVal: Statement.Function, val env : MutableMap<String, Value>) : Value(functionVal)
    class ListVal(listVal: HashMap<Int, Value>): Value(listVal)

    fun intVal():Int{
      if (this is NumVal){
        return value as Int
      } else {
        throw CastException
      }
    }

    fun textVal(): String {
      if (this is TextVal){
        return value as String
      } else {
        throw CastException
      }
    }

    fun boolVal():Boolean{
      if (this is BoolVal){
        return value as Boolean
      } else {
        throw CastException
      }
    }

    fun listVal(): HashMap<Int, Value> {
      if (this is ListVal){
        return value as HashMap<Int, Value>
      } else {
        throw CastException
      }
    }

    fun functionVal(): Statement.Function {
      if (this is FunctionVal){
        return value as Statement.Function
      } else {
        throw CastException
      }
    }

    fun copyOrReference(): Value{
      when(this){
        is BoolVal -> return BoolVal(this.value as Boolean)
        is NumVal -> return NumVal(this.value as Int)
        else -> return this
      }
    }
  }

  data class TypeMismatch(override val message: String) : RuntimeException(message)
  object CastException: Exception()
  data class UndefinedIdentifier(override val message: String) : RuntimeException("Undeclared identifier '$message'")
  data class VariableAlreadyDefined(val identifier: String) : RuntimeException("Identifier aready declared '$identifier'")
  object InvalidUserInput : RuntimeException("Please enter a number of the value 'true' or 'false'")
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
      is Statement.If -> return evalIf(statement.condition, statement.body.statementList, null, HashMap(env))
      is Statement.IfElse -> return evalIf(statement.condition, statement.ifBody.statementList, statement.elseBody.statementList, HashMap(env))
      is Statement.While -> return evalWhile(statement.condition, statement.body.statementList, HashMap(env))
      is Statement.ValDeclaration -> {
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
          list[index] = value.copyOrReference()
        } else {
          throw UndefinedIdentifier(identifier)
        }

        return null
      }
      is Statement.Input -> {
        //TODO make this work in JS and JVM envs
//        val identifier = statement.identifier.name
//        val input = inputReader.readLine()
//        if (input == null || input.isEmpty()) {
//          throw InvalidUserInput
//        } else if ("true" == input) {
//          setValueInEnv(env, identifier, Value.BoolVal(true))
//        } else if ("false" == input) {
//          setValueInEnv(env, identifier, Value.BoolVal(false))
//        } else {
//          setValueInEnv(env, identifier, Value.NumVal(input.toInt()))
//        }
        return null
      }
      is Statement.Output -> {
        //TODO make this work with io redirection
        val value = eval(statement.expression, env)
        println(value.value)
        return null
      }
      is Statement.Return -> {
        return eval(statement.expression, env)
      }
    }
  }

  fun setValueInEnv(env: MutEnv, identifier: String, value: Value){
    if (env.containsKey(identifier)){
      val existingValue = env.getValue(identifier)
      if (existingValue::class != value::class) {
        throw TypeMismatch("$identifier was already set to $existingValue, new value was $value")
      }
      env.getValue(identifier).value = value.value
    } else {
      env[identifier] = value
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

  fun evalCondition(condition: Expression, env: Env) : Value.BoolVal {
    val value = eval(condition, env)
    if (value is Value.BoolVal) {
      return value
    } else {
      throw TypeMismatch("condition did not return boolean result '$condition'")
    }
  }

  fun eval(expression: Expression, env: Env): Value {
    when (expression) {
      is Expression.Num -> return Value.NumVal(expression.value)
      is Expression.Bool -> return Value.BoolVal(expression.value)
      is Expression.Text -> return Value.TextVal(expression.value)
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

  fun evalList(expression: Expression.ListDeclaration, env: Env): Value.ListVal {
    val list = HashMap<Int, Value>()
    expression.items.forEachIndexed() { index, expr->
      list.put(index, eval(expr, env))
    }

    return Value.ListVal(list)
  }

  fun evalListAccess(expression: Expression.ListAccess, env: Env): Value{
    val list = eval(expression.listExpression, env)
    val index = eval(expression.indexExpression, env).intVal()
    return list.listVal().getValue(index)
  }


  fun callFunction(functionCall: Expression.FunctionCall, env: Env) : Eval.Value{
    val functionVal = getFunction(functionCall, env)
    val funEnv = getFunctionCallEnv(functionVal.functionVal(), functionCall, functionVal.env, env)

    return evalFunction(functionVal.functionVal().body.statementList, funEnv)
  }

  fun getFunction(functionCall: Expression.FunctionCall, env: Env): Value.FunctionVal {

    val function = eval(functionCall.functionExpression, env)

    if (function !is Value.FunctionVal){
      throw TypeMismatch("$function is not callable")
    }

    return function
  }

  fun getFunctionCallEnv(function : Statement.Function, functionCall: Expression.FunctionCall, functionDefEnv: Env, functionCallEnv: Env) : MutEnv {
    if (functionCall.params.size != function.params.size){
      throw TypeMismatch("Wrong number of arguments to call $function ")
    }

    val functionRunEnv = HashMap<String, Value>()
    functionRunEnv.putAll(functionDefEnv)

    functionCall.params.forEachIndexed({index, expression ->
      val paramVal : Value = eval(expression, functionCallEnv)
      functionRunEnv.put(function.params[index].name, paramVal)
    })

    return functionRunEnv
  }

  fun evalFunction(body: List<Statement>, env: MutEnv) : Eval.Value {
    body.forEach{
      val value = eval(it, env)
      if (value != null){
        return value
      }
    }
    throw FunctionExitedWithoutReturn
  }

  fun applyBinaryOperator(operatorExpression: Expression.BinaryOperator, env: Env): Value {
    val operation = operatorExpression.operator
    val lhsVal = eval(operatorExpression.lhs, env)
    val rhsVal = eval(operatorExpression.rhs, env)
    try {
      when (operation) {
        BinaryOperators.Add -> return Value.NumVal(lhsVal.intVal() + rhsVal.intVal())
        BinaryOperators.Sub -> return Value.NumVal(lhsVal.intVal() - rhsVal.intVal())
        BinaryOperators.Div -> return Value.NumVal(lhsVal.intVal() / rhsVal.intVal())
        BinaryOperators.Mul -> return Value.NumVal(lhsVal.intVal() * rhsVal.intVal())
        BinaryOperators.Equality -> return Value.BoolVal(lhsVal.intVal() == rhsVal.intVal())
        BinaryOperators.NotEquality -> return Value.BoolVal(lhsVal.intVal() != rhsVal.intVal())
        BinaryOperators.LessThan -> return Value.BoolVal(lhsVal.intVal() < rhsVal.intVal())
        BinaryOperators.GreaterThan -> return Value.BoolVal(lhsVal.intVal() > rhsVal.intVal())
        BinaryOperators.LessThanEq -> return Value.BoolVal(lhsVal.intVal() <= rhsVal.intVal())
        BinaryOperators.GreaterThanEq -> return Value.BoolVal(lhsVal.intVal() >= rhsVal.intVal())
        BinaryOperators.And -> return Value.BoolVal(lhsVal.boolVal() && rhsVal.boolVal())
        BinaryOperators.Or -> return Value.BoolVal(lhsVal.boolVal() || rhsVal.boolVal())
      }
    } catch (e: CastException) {
      throw TypeMismatch("$operation cannot be applied to $lhsVal and $rhsVal")
    }
  }

  fun applyUnaryOperator(operatorExpr: Expression.UnaryOperator, env: Env): Value{
    val result = eval(operatorExpr.expression, env)
    val operator = operatorExpr.operator
    try {
      when(operator){
        UnaryOperators.Not -> return Value.BoolVal(!result.boolVal())
        UnaryOperators.Negative -> return Value.NumVal(-result.intVal())
      }
    } catch (e: CastException) {
      throw TypeMismatch("$operator cannot be applied to $result")
    }
  }

  fun evalIf(condition: Expression, ifBody: List<Statement>, elseBody: List<Statement>?, env: MutEnv) : Value? {
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
      throw TypeMismatch("If statement condition type error expected bool got $condition")
    }
    return null
  }
}
