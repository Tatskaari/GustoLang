package tatskaari.eval

import tatskaari.parsing.Expression
import tatskaari.parsing.Statement
import tatskaari.tokenising.Operator

object Eval {
  sealed class Value(val value : Any) {
    data class NumVal(val intVal : Int) : Value(intVal)
    data class BoolVal(val boolVal : Boolean) : Value(boolVal)
  }

  data class TypeMismatch(override val message : String) : RuntimeException(message)
  data class UndefinedIdentifier(override val message : String) : RuntimeException("Undeclared identifier '$message'")

  fun eval(statements : List<Statement>, env: MutableMap<String, Value>) {
    statements.forEach { eval(it, env) }
  }

  fun eval(statement : Statement, env: MutableMap<String, Value>) {
    when (statement) {
      is Statement.CodeBlock -> eval(statement.statementList, env)
      is Statement.If -> {
        val condition = statement.condition
        val conditionResult = eval(condition, env)
        if (conditionResult is Value.BoolVal){
          if (conditionResult.boolVal){
            eval(statement.body, env)
          }
        } else {
          throw TypeMismatch("If statement condition type error expected bool got $condition")
        }
      }
      is Statement.Assignment -> {
        val identifier = statement.identifier.name
        val value = eval(statement.expression, env)
        if (env.containsKey(identifier)){
          val existingValue = env.getValue(identifier)::class
          if (env.getValue(identifier)::class != value::class){
            throw TypeMismatch("$identifier was already set to $existingValue, new value was $value")
          }
        }
        env[identifier] = value
      }
    }
  }

  fun eval(expression : Expression, env : Map<String, Value>) : Value {
    when(expression) {
      is Expression.Num -> return Value.NumVal(expression.value)
      is Expression.Op -> return applyOperator(expression, env)
      is Expression.Identifier -> {
        val value = env[expression.name]
        if (value != null){
          return value
        } else {
          throw UndefinedIdentifier(expression.name)
        }
      }
    }
  }

  fun applyOperator(operatorExpression : Expression.Op, env: Map<String, Value>) : Value {
    val operation = operatorExpression.operator
    val lhsVal = eval(operatorExpression.lhs, env)
    val rhsVal = eval(operatorExpression.rhs, env)

    fun applyNumOp() : Value.NumVal{
      if (lhsVal is Value.NumVal && rhsVal is Value.NumVal) {
        when (operation) {
          is Operator.Add -> return Value.NumVal(lhsVal.intVal + rhsVal.intVal)
          is Operator.Sub -> return Value.NumVal(lhsVal.intVal - rhsVal.intVal)
        }
      }
      throw TypeMismatch("Number operator applied to $lhsVal and $rhsVal")
    }

    fun applyBoolOp() : Value.BoolVal {
      when(operation){
        is Operator.Equality -> return Value.BoolVal(lhsVal.value == rhsVal.value)
      }
      throw TypeMismatch("Boolean operator applied to $lhsVal and $rhsVal")
    }

    when(operation.resultType){
      Operator.ResultType.BOOLEAN -> return applyBoolOp()
      else -> return applyNumOp()
    }

  }
}