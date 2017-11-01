package tatskaari

import tatskaari.compatibility.random
import tatskaari.eval.*
import tatskaari.eval.values.Value
import tatskaari.GustoType.*

enum class BuiltInFunction(val funName: String, val params: List<String>, val type: FunctionType, val function: (MutEnv) -> Value) {
  SizeOfList(
    "size",
    listOf("list"),
    FunctionType(listOf(ListType(UnknownType)), PrimitiveType.Integer),
    { params -> Value.IntVal(params.getValue("list").listVal().entries.size) }
  ),
  ToInt(
    "toInteger",
    listOf("textVal"),
    FunctionType(listOf(PrimitiveType.Text), PrimitiveType.Integer),
    {params -> Value.IntVal(params.getValue("textVal").textVal().toInt())}
  ),
  ToNum(
    "toNumber",
    listOf("textVal"),
    FunctionType(listOf(PrimitiveType.Text), PrimitiveType.Number),
    {params -> Value.NumVal(params.getValue("textVal").textVal().toDouble())}
  ),
  ToBoolean(
    "toBoolean",
    listOf("textVal"),
    FunctionType(listOf(PrimitiveType.Text), PrimitiveType.Boolean),
    { params ->
      val text = params.getValue("textVal").textVal()
      when (text){
        "true" -> Value.BoolVal(true)
        "false" -> Value.BoolVal(false)
        else -> throw Eval.InvalidUserInput
      }
    }
  ),
  Random(
    "random",
    listOf(),
    FunctionType(listOf(), PrimitiveType.Number),
    { Value.NumVal(random()) }
  );

  companion object {
    fun getTypeEnv(): HashMap<String, GustoType> {
      val typeEnv: HashMap<String, GustoType> = HashMap()
      typeEnv.putAll(BuiltInFunction.values().map{Pair(it.funName, it.type)})
      return typeEnv
    }

    fun getEvalEnv(): MutEnv {
      val evalEnv: MutEnv = MutEnv()
      evalEnv.putAll(BuiltInFunction.values().map{ Pair(it.funName, Value.BifVal(it))})
      return evalEnv
    }
  }
}
