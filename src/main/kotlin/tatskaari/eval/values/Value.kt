package tatskaari.eval.values

import tatskaari.eval.Eval
import tatskaari.parsing.Statement

sealed class Value(var value: Any) {
  interface Addable {
    infix fun plus(value: Addable): Value
  }

  interface Subtractable {
    infix fun minus(value: Subtractable): Value
  }

  interface Multiplicable {
    infix fun times(value: Multiplicable): Value
  }

  interface Divisible {
    infix fun div(value: Divisible): Value
  }

  class IntVal(intVal: Int) : Value(intVal), Addable, Subtractable, Multiplicable, Divisible {
    override fun plus(value: Addable): Value {
      return when(value){
        is IntVal -> IntVal(intVal() + value.intVal())
        is NumVal -> NumVal(intVal() + value.numVal())
        else -> throw Eval.TypeMismatch("You cannot add $this and $value")
      }
    }

    override fun minus(value: Subtractable): Value {
      return when(value){
        is IntVal -> IntVal(intVal() - value.intVal())
        is NumVal -> NumVal(intVal() - value.numVal())
        else -> throw Eval.TypeMismatch("You cannot subtract $this and $value")
      }
    }

    override fun div(value: Divisible): Value {
      return when(value){
        is IntVal -> NumVal(intVal().toDouble() / value.intVal().toDouble())
        is NumVal -> NumVal(intVal().toDouble() / value.numVal())
        else -> throw Eval.TypeMismatch("You cannot divide $this and $value")
      }
    }

    override fun times(value: Multiplicable): Value {
      return when(value){
        is IntVal -> IntVal(intVal() * value.intVal())
        is NumVal -> NumVal(intVal() * value.numVal())
        else -> throw Eval.TypeMismatch("You cannot multiply $this and $value")
      }
    }
  }

  class NumVal(numVal: Double): Value(numVal), Addable, Subtractable, Multiplicable, Divisible {
    override fun div(value: Divisible): Value {
      return when(value){
        is IntVal -> NumVal(numVal() / value.intVal())
        is NumVal -> NumVal(numVal() / value.numVal())
        else -> throw Eval.TypeMismatch("You cannot divide $this and $value")
      }
    }

    override fun times(value: Multiplicable): Value {
      return when(value){
        is IntVal -> NumVal(numVal() * value.intVal())
        is NumVal -> NumVal(numVal() * value.numVal())
        else -> throw Eval.TypeMismatch("You cannot multiply $this and $value")
      }
    }

    override fun plus(value: Addable): Value {
      return when(value){
        is IntVal -> NumVal(numVal() + value.intVal())
        is NumVal -> NumVal(numVal() + value.numVal())
        else -> throw Eval.TypeMismatch("You cannot add $this and $value")
      }
    }

    override fun minus(value: Subtractable): Value {
      return when(value){
        is IntVal -> NumVal(numVal() - value.intVal())
        is NumVal -> NumVal(numVal() - value.numVal())
        else -> throw Eval.TypeMismatch("You cannot subtract $this and $value")
      }
    }
  }
  class TextVal(textVal: String) : Value(textVal), Addable {
    override fun plus(value: Addable): Value = TextVal(textVal() + value.toString() )
  }

  class BoolVal(boolVal: Boolean) : Value(boolVal)

  class FunctionVal(functionVal: Statement.Function, val env : MutableMap<String, Value>) : Value(functionVal)

  class ListVal(listVal: HashMap<Int, Value>): Value(listVal)

  fun intVal():Int{
    if (this is IntVal){
      return value as Int
    } else {
      throw Eval.CastException
    }
  }

  fun numVal():Double {
    if (this is NumVal){
      return value as Double
    } else {
      throw Eval.CastException
    }
  }

  fun textVal(): String {
    if (this is TextVal){
      return value as String
    } else {
      throw Eval.CastException
    }
  }

  fun boolVal():Boolean{
    if (this is BoolVal){
      return value as Boolean
    } else {
      throw Eval.CastException
    }
  }

  fun listVal(): HashMap<Int, Value> {
    if (this is ListVal){
      return value as HashMap<Int, Value>
    } else {
      throw Eval.CastException
    }
  }

  fun functionVal(): Statement.Function {
    if (this is FunctionVal){
      return value as Statement.Function
    } else {
      throw Eval.CastException
    }
  }

  fun copyLiteralOrReferenceList(): Value{
    when(this){
      is BoolVal -> return BoolVal(this.value as Boolean)
      is IntVal -> return IntVal(this.value as Int)
      else -> return this
    }
  }

  override fun toString(): String = value.toString()
}