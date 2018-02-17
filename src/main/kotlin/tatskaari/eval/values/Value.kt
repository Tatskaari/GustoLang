package tatskaari.eval.values

import tatskaari.BuiltInFunction
import tatskaari.eval.Eval
import tatskaari.parsing.Expression
import tatskaari.eval.EvalEnv


interface Addable {
  infix fun plus(value: Addable): Value
}

interface Stringable : Addable {
  override fun plus(value: Addable): Value {
    if (value is Value.TextVal){
      return Value.TextVal(toString() + value.toString())
    }
    throw Eval.TypeMismatch("You cannot add $this and $value")
  }
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

sealed class Value(var value: Any) {
  object Unit : Value(kotlin.Unit)

  class IntVal(intVal: Int) : Value(intVal), Addable, Subtractable, Multiplicable, Divisible {
    override fun plus(value: Addable): Value {
      return when(value){
        is IntVal -> IntVal(intVal() + value.intVal())
        is NumVal -> NumVal(intVal() + value.doubleVal())
        is TextVal -> TextVal( toString() + value.toString())
        else -> throw Eval.TypeMismatch("You cannot add $this and $value")

      }
    }

    override fun minus(value: Subtractable): Value {
      return when(value){
        is IntVal -> IntVal(intVal() - value.intVal())
        is NumVal -> NumVal(intVal() - value.doubleVal())
        else -> throw Eval.TypeMismatch("You cannot subtract $this and $value")
      }
    }

    override fun div(value: Divisible): Value {
      return when(value){
        is IntVal -> NumVal(intVal().toDouble() / value.intVal().toDouble())
        is NumVal -> NumVal(intVal().toDouble() / value.doubleVal())
        else -> throw Eval.TypeMismatch("You cannot divide $this and $value")
      }
    }

    override fun times(value: Multiplicable): Value {
      return when(value){
        is IntVal -> IntVal(intVal() * value.intVal())
        is NumVal -> NumVal(intVal() * value.doubleVal())
        else -> throw Eval.TypeMismatch("You cannot multiply $this and $value")
      }
    }

    override fun equals(other: Any?): Boolean {
      if (other is Value) {
        // integers and doubles should be considered equal in gusto if they contain the same value (unlike Kotlin)
        return this.intVal() == other.value
      }
      return false
    }
  }

  class NumVal(numVal: Double): Value(numVal), Addable, Subtractable, Multiplicable, Divisible {
    override fun div(value: Divisible): Value {
      return when(value){
        is IntVal -> NumVal(doubleVal() / value.intVal())
        is NumVal -> NumVal(doubleVal() / value.doubleVal())
        else -> throw Eval.TypeMismatch("You cannot divide $this and $value")
      }
    }

    override fun times(value: Multiplicable): Value {
      return when(value){
        is IntVal -> NumVal(doubleVal() * value.intVal())
        is NumVal -> NumVal(doubleVal() * value.doubleVal())
        else -> throw Eval.TypeMismatch("You cannot multiply $this and $value")
      }
    }

    override fun plus(value: Addable): Value {
      return when(value){
        is IntVal -> NumVal(doubleVal() + value.intVal())
        is NumVal -> NumVal(doubleVal() + value.doubleVal())
        is TextVal -> TextVal( toString() + value.toString())
        else -> throw Eval.TypeMismatch("You cannot add $this and $value")
      }
    }

    override fun minus(value: Subtractable): Value {
      return when(value){
        is IntVal -> NumVal(doubleVal() - value.intVal())
        is NumVal -> NumVal(doubleVal() - value.doubleVal())
        else -> throw Eval.TypeMismatch("You cannot subtract $this and $value")
      }
    }
  }

  class TextVal(textVal: String) : Value(textVal), Addable {
    override fun plus(value: Addable): Value = TextVal(textVal() + value.toString())
  }

  class BoolVal(boolVal: Boolean) : Value(boolVal), Stringable

  class FunctionVal(functionVal: Expression.Function, val env : EvalEnv) : Value(functionVal)
  class BifVal(val bif: BuiltInFunction): Value(bif)

  class ListVal(listVal: HashMap<Int, Value>): Value(listVal)

  class VariantVal(val name: String, val params : Value) : Value(name) {
    override fun equals(other: Any?): Boolean {
      if (other is VariantVal){
        if (other.name != this.name){
          return false
        }
        return other.params == params
      } else {
        return false
      }
    }

    override fun hashCode(): Int {
      var result = name.hashCode()
      result = 31 * result + (params?.hashCode() ?: 0)
      return result
    }
  }

  fun intVal():Int{
    if (this is IntVal){
      return value as Int
    } else {
      throw Eval.CastException
    }
  }

  fun doubleVal():Double {
    if (this is NumVal){
      return value as Double
    } else {
      throw Eval.CastException
    }
  }

  fun numVal(): Number {
    if (this.value is Number){
      return value as Number
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

  fun tupleVal(): List<Value> {
    if (this is TupleVal) {
      return value as List<Value>
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

  fun functionVal(): Expression.Function {
    if (this is FunctionVal){
      return value as Expression.Function
    } else {
      throw Eval.CastException
    }
  }

  fun copyLiteralOrReferenceList(): Value{
    return when(this){
      is BoolVal -> BoolVal(this.value as Boolean)
      is IntVal -> IntVal(this.value as Int)
      is TextVal -> TextVal(this.value as String)
      is NumVal -> NumVal(this.value as Double)
      else -> this
    }
  }

  override fun equals(other: Any?): Boolean {
    if (other is Value) {
      return value == other.value
    }
    return false
  }

  override fun toString(): String = value.toString()

  class TupleVal(val values: List<Value>) : Value(values) {
    override fun equals(other: Any?): Boolean {
      return other is TupleVal && other.values.size == values.size && !other.values.zip(values).any {(other, it) -> other != it}
    }
  }
}