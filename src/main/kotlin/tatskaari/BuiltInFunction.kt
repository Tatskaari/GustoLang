package tatskaari

import tatskaari.compatibility.random
import tatskaari.eval.*
import tatskaari.eval.values.Value
import tatskaari.GustoType.*

enum class BuiltInFunction(val funName: String, val params: List<String>, val type: FunctionType, val function: (EvalEnv) -> Value) {
  SizeOfList(
    "size",
    listOf("list"),
    FunctionType(listOf(ListType(UnknownType)), PrimitiveType.Integer),
    { params -> Value.IntVal(params["list"].listVal().entries.size) }
  ),
  ToInt(
    "toInteger",
    listOf("textVal"),
    FunctionType(listOf(PrimitiveType.Text), PrimitiveType.Integer),
    {params -> Value.IntVal(params["textVal"].textVal().toInt())}
  ),
  ToNum(
    "toNumber",
    listOf("textVal"),
    FunctionType(listOf(PrimitiveType.Text), PrimitiveType.Number),
    {params -> Value.NumVal(params["textVal"].textVal().toDouble())}
  ),
  ToBoolean(
    "toBoolean",
    listOf("textVal"),
    FunctionType(listOf(PrimitiveType.Text), PrimitiveType.Boolean),
    { params ->
      val text = params["textVal"].textVal()
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
    fun getTypeEnv(): tatskaari.parsing.typechecking.TypeEnv {
      val typeEnv: HashMap<String, GustoType> = HashMap()
      typeEnv.putAll(BuiltInFunction.values().map{Pair(it.funName, it.type)})
      return tatskaari.parsing.typechecking.TypeEnv(typeEnv, HashMap())
    }

    fun getEvalEnv(): EvalEnv {
      val evalEnv = EvalEnv()
      evalEnv.putAll(BuiltInFunction.values().map{ Pair(it.funName, Value.BifVal(it))})
      return evalEnv
    }
  }
}
