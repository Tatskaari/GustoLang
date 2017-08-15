package tatskaari.eval

import tatskaari.parsing.Expression
import tatskaari.parsing.Statement
import tatskaari.tokenising.Operator
import java.io.BufferedReader
import java.io.PrintStream

class Eval(val inputReader: BufferedReader, val outputStream: PrintStream) {
  constructor() : this(System.`in`.bufferedReader(), System.out)
  constructor(inputReader: BufferedReader) : this(inputReader, System.out)
  constructor(outputStream: PrintStream) : this(System.`in`.bufferedReader(), outputStream)


  sealed class Value(val value: Any) {
    data class NumVal(val intVal: Int) : Value(intVal)
    data class BoolVal(val boolVal: Boolean) : Value(boolVal)
    data class FunctionVal(val function: Statement.Function, val env : MutableMap<String, Value>) : Value(function)
  }

  data class TypeMismatch(override val message: String) : RuntimeException(message)
  data class UndefinedIdentifier(override val message: String) : RuntimeException("Undeclared identifier '$message'")
  data class VariableAlreadyDefined(val identifier: String) : RuntimeException("Identifier aready declared '$identifier'")
  object InvalidUserInput : RuntimeException("Please enter a number of the value 'true' or 'false'")
  //TODO better error message
  object FunctionExitedWithoutReturn : RuntimeException("Function exited without return")


  fun eval(statements: List<Statement>, env: MutableMap<String, Value>) : Value?  {
    for (statement in statements) {
      val value = eval(statement, env)
      if (value != null) {
        return value
      }
    }
    return null
  }

  fun eval(statement: Statement, env: MutableMap<String, Value>) : Value? {
    when (statement) {
      is Statement.CodeBlock -> return eval(statement.statementList, env)
      is Statement.If -> return evalIf(statement.condition, statement.body, null, env)
      is Statement.IfElse -> return evalIf(statement.condition, statement.ifBody, statement.elseBody, env)
      is Statement.While -> return evalWhile(statement.condition, statement.body, env)
      is Statement.ValDeclaration -> {
        val identifierName = statement.identifier.name

        if (env.containsKey(identifierName)){
          throw VariableAlreadyDefined(identifierName)
        } else {
          env[identifierName] = eval(statement.expression, env)
        }
      }
      is Statement.Function -> {
        val identifierName = statement.identifier.name
        if (env.containsKey(identifierName)){
          throw VariableAlreadyDefined(identifierName)
        } else {
          env[identifierName] = Value.FunctionVal(statement, env)
        }
      }
      is Statement.Assignment -> {
        val identifier = statement.identifier.name
        val value = eval(statement.expression, env)
        if (env.containsKey(identifier)) {
          val existingValue = env.getValue(identifier)
          if (existingValue::class != value::class) {
            throw TypeMismatch("$identifier was already set to $existingValue, new value was $value")
          }
          env[identifier] = value
        } else {
          throw UndefinedIdentifier(identifier)
        }
      }
      is Statement.Input -> {
        val identifier = statement.identifier.name
        val input = inputReader.readLine()
        if (input == null || input.isEmpty()) {
          throw InvalidUserInput
        } else if ("true" == input) {
          env[identifier] = Value.BoolVal(true)
        } else if ("false" == input) {
          env[identifier] = Value.BoolVal(false)
        } else {
          env[identifier] = Value.NumVal(input.toInt())
        }
      }
      is Statement.Output -> {
        val value = eval(statement.expression, env)
        outputStream.println(value.value)
      }
      is Statement.Return -> {
        return eval(statement.expression, env)
      }
    }

    return null
  }

  private fun evalWhile(condition: Expression, body: List<Statement>, env: MutableMap<String, Value>) : Value? {
    while(evalCondition(condition, env).boolVal){
      val value : Value? = eval(body, env)
      if (value != null) {
        return value
      }
    }
    return null
  }

  fun evalCondition(condition: Expression, env: Map<String, Value>) : Value.BoolVal {
    val value = eval(condition, env)
    if (value is Value.BoolVal) {
      return value
    } else {
      throw TypeMismatch("condition did not return boolean result '$condition'")
    }
  }

  fun eval(expression: Expression, env: Map<String, Value>): Value {
    when (expression) {
      is Expression.Num -> return Value.NumVal(expression.value)
      is Expression.Bool -> return Value.BoolVal(expression.value)
      is Expression.Op -> return applyOperator(expression, env)
      is Expression.Not -> return Value.BoolVal(!evalCondition(expression.expr, env).boolVal)
      is Expression.And -> return Value.BoolVal(evalCondition(expression.lhs, env).boolVal && evalCondition(expression.rhs, env).boolVal)
      is Expression.Or -> return Value.BoolVal(evalCondition(expression.lhs, env).boolVal || evalCondition(expression.rhs, env).boolVal)
      is Expression.FunctionCall -> return callFunction(expression, env)
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

  fun callFunction(functionCall: Expression.FunctionCall, env: Map<String, Value>) : Eval.Value{
    val functionVal = getFunction(functionCall, env)
    val funEnv = getFunctionCallEnv(functionVal.function, functionCall, functionVal.env, env)

    return evalFunction(functionVal.function.body, funEnv)
  }

  fun getFunction(functionCall: Expression.FunctionCall, env: Map<String, Value>): Value.FunctionVal {
    if (!env.containsKey(functionCall.functionIdentifier.name)){
      throw UndefinedIdentifier(functionCall.functionIdentifier.name)
    }

    val function = env[functionCall.functionIdentifier.name]

    if (function !is Value.FunctionVal){
      throw TypeMismatch("$function is not callable")
    }

    return function
  }

  fun getFunctionCallEnv(function : Statement.Function, functionCall: Expression.FunctionCall, functionDefEnv: Map<String, Value>, functionCallEnv: Map<String, Value>) : HashMap<String, Value> {
    if (functionCall.params.size != function.params.size){
      throw TypeMismatch("Wrong number of arguments to call $function ")
    }

    val functionRunEnv = HashMap<String, Value>()
    functionRunEnv.putAll(functionDefEnv)

    functionCall.params.forEach({
      val identifier = it.identifier
      if(function.params.contains(identifier)){
        val paramVal : Value = eval(it.expression, functionCallEnv)
        functionRunEnv.put(identifier.name, paramVal)
      } else {
        throw TypeMismatch("$function doesn't contain the parameter $identifier")
      }
    })

    return functionRunEnv
  }

  fun evalFunction(body: List<Statement>, env: MutableMap<String, Value>) : Eval.Value {
    for (statement in body){
      val value = eval(statement, env)
      if (value != null){
        return value
      }
    }
    throw FunctionExitedWithoutReturn
  }

  fun applyOperator(operatorExpression: Expression.Op, env: Map<String, Value>): Value {
    val operation = operatorExpression.operator
    val lhsVal = eval(operatorExpression.lhs, env)
    val rhsVal = eval(operatorExpression.rhs, env)

    if (lhsVal is Value.NumVal && rhsVal is Value.NumVal) {
      when (operation) {
        Operator.Add -> return Value.NumVal(lhsVal.intVal + rhsVal.intVal)
        Operator.Sub -> return Value.NumVal(lhsVal.intVal - rhsVal.intVal)
        Operator.Div -> return Value.NumVal(lhsVal.intVal / rhsVal.intVal)
        Operator.Mul -> return Value.NumVal(lhsVal.intVal * rhsVal.intVal)
        Operator.Equality -> return Value.BoolVal(lhsVal.intVal == rhsVal.intVal)
        Operator.LessThan -> return Value.BoolVal(lhsVal.intVal < rhsVal.intVal)
        Operator.GreaterThan -> return Value.BoolVal(lhsVal.intVal > rhsVal.intVal)
        Operator.LessThanEq -> return Value.BoolVal(lhsVal.intVal <= rhsVal.intVal)
        Operator.GreaterThanEq -> return Value.BoolVal(lhsVal.intVal >= rhsVal.intVal)
      }
    } else {
      throw TypeMismatch("Number operator applied to $lhsVal and $rhsVal")
    }
  }

  fun evalIf(condition: Expression, ifBody: List<Statement>, elseBody: List<Statement>?, env: MutableMap<String, Value>) : Value? {
    val conditionResult = eval(condition, env)
    if (conditionResult is Value.BoolVal) {
      var value : Value? = null
      if (conditionResult.boolVal) {
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
