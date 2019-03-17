package tatskaari

import tatskaari.GustoType.*
import tatskaari.compatibility.random
import tatskaari.eval.Eval
import tatskaari.eval.EvalEnv
import tatskaari.eval.values.Value
import tatskaari.parsing.hindleymilner.Type
import tatskaari.parsing.hindleymilner.TypeEnv
import kotlin.math.pow

enum class BuiltInFunction(val funName: String, val params: List<String>, val type: FunctionType, val hmType: Type.Scheme, val function: (EvalEnv) -> Value) {
  SizeOfList(
    "size",
    listOf("list"),
    FunctionType(listOf(ListType(UnknownType)), PrimitiveType.Integer),
    Type.Scheme(listOf("bif$1"), Type.Function(Type.ListType(Type.Var("bif$1")), Type.Int)),
      { params -> Value.IntVal(params["list"].listVal().entries.size) }
    ),
  ToInt(
    "toInteger",
    listOf("textVal"),
    FunctionType(listOf(PrimitiveType.Text), PrimitiveType.Integer),
    Type.Scheme(listOf(), Type.Function(Type.Text, Type.Int)),
    {params -> Value.IntVal(params["textVal"].textVal().toInt())}
  ),
  ToNum(
    "toNumber",
    listOf("textVal"),
    FunctionType(listOf(PrimitiveType.Text), PrimitiveType.Number),
    Type.Scheme(listOf(), Type.Function(Type.Text, Type.Num)),
    {params -> Value.NumVal(params["textVal"].textVal().toDouble())}
  ),
  ToBoolean(
    "toBoolean",
    listOf("textVal"),
    FunctionType(listOf(PrimitiveType.Text), PrimitiveType.Boolean),
    Type.Scheme(listOf(), Type.Function(Type.Text, Type.Bool)),
    { params ->
      val text = params["textVal"].textVal()
      when (text){
        "true" -> Value.BoolVal(true)
        "false" -> Value.BoolVal(false)
        else -> throw Eval.InvalidUserInput
      }
    }
  ),
  Pow(
    "pow",
    listOf("base", "exponent"),
    FunctionType(listOf(PrimitiveType.Number, PrimitiveType.Number), PrimitiveType.Number),
    Type.Scheme(listOf(), Type.Function(Type.Num, Type.Function(Type.Num, Type.Num))),
    {
      Value.NumVal(
        it["base"].numVal().toDouble()
          .pow(it["exponent"].numVal().toDouble())
      )
    }
  ),
  Random(
    "random",
    listOf(),
    FunctionType(listOf(), PrimitiveType.Number),
    Type.Scheme(listOf(), Type.Function(Type.Unit, Type.Int)),
    { Value.NumVal(random()) }
  );

  companion object {
    fun getTypeEnv(): tatskaari.parsing.typechecking.TypeEnv {
      val typeEnv: HashMap<String, GustoType> = HashMap()
      typeEnv.putAll(BuiltInFunction.values().map{Pair(it.funName, it.type)})
      return tatskaari.parsing.typechecking.TypeEnv(typeEnv, HashMap())
    }

    fun getHindleyMilnerEnv() : TypeEnv {
      val typeEnv: HashMap<String, Type.Scheme> = HashMap()
      typeEnv.putAll(BuiltInFunction.values().map{Pair(it.funName, it.hmType)})
      return TypeEnv(typeEnv)
    }

    fun getEvalEnv(): EvalEnv {
      val evalEnv = EvalEnv()
      evalEnv.putAll(BuiltInFunction.values().map{ Pair(it.funName, Value.BifVal(it))})
      return evalEnv
    }
  }
}
