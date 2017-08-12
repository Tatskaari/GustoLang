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
    object NullVal : Value(NullVal)
  }

  data class TypeMismatch(override val message: String) : RuntimeException(message)
  data class UndefinedIdentifier(override val message: String) : RuntimeException("Undeclared identifier '$message'")

  fun eval(statements: List<Statement>, env: MutableMap<String, Value>) {
    statements.forEach { eval(it, env) }
  }

  fun eval(statement: Statement, env: MutableMap<String, Value>) {
    when (statement) {
      is Statement.CodeBlock -> eval(statement.statementList, env)
      is Statement.If -> evalIf(statement.condition, statement.body, null, env)
      is Statement.IfElse -> evalIf(statement.condition, statement.ifBody, statement.elseBody, env)
      is Statement.Assignment -> {
        val identifier = statement.identifier.name
        val value = eval(statement.expression, env)
        if (env.containsKey(identifier)) {
          val existingValue = env.getValue(identifier)::class
          if (env.getValue(identifier)::class != value::class) {
            throw TypeMismatch("$identifier was already set to $existingValue, new value was $value")
          }
        }
        env[identifier] = value
      }
      is Statement.Input -> {
        val identifier = statement.identifier.name
        val input = inputReader.readLine()
        if (input == null || input.isEmpty()) {
          env[identifier] = Value.NullVal
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
    }
  }

  fun eval(expression: Expression, env: Map<String, Value>): Value {
    when (expression) {
      is Expression.Num -> return Value.NumVal(expression.value)
      is Expression.Bool -> return Value.BoolVal(expression.value)
      is Expression.Op -> return applyOperator(expression, env)
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

  fun applyOperator(operatorExpression: Expression.Op, env: Map<String, Value>): Value {
    val operation = operatorExpression.operator
    val lhsVal = eval(operatorExpression.lhs, env)
    val rhsVal = eval(operatorExpression.rhs, env)

    fun applyNumOp(): Value.NumVal {
      if (lhsVal is Value.NumVal && rhsVal is Value.NumVal) {
        when (operation) {
          is Operator.Add -> return Value.NumVal(lhsVal.intVal + rhsVal.intVal)
          else -> return Value.NumVal(lhsVal.intVal - rhsVal.intVal)
        }
      } else {
        throw TypeMismatch("Number operator applied to $lhsVal and $rhsVal")
      }
    }

    fun applyBoolOp(): Value.BoolVal {
      //TODO deal with other boolean operations
      return Value.BoolVal(lhsVal.value == rhsVal.value)
    }

    when (operation.resultType) {
      Operator.ResultType.BOOLEAN -> return applyBoolOp()
      else -> return applyNumOp()
    }

  }

  fun evalIf(condition: Expression, ifBody: List<Statement>, elseBody: List<Statement>?, env: MutableMap<String, Value>) {
    val conditionResult = eval(condition, env)
    if (conditionResult is Value.BoolVal) {
      if (conditionResult.boolVal) {
        eval(ifBody, env)
      } else if (elseBody != null) {
        eval(elseBody, env)
      }
    } else {
      throw TypeMismatch("If statement condition type error expected bool got $condition")
    }

  }
}