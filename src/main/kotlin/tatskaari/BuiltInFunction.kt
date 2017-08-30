package tatskaari

import tatskaari.eval.MutEnv
import tatskaari.eval.values.Value

enum class BuiltInFunction(val funName: String, val params: List<String>, val type: FunctionType, val function: (MutEnv) -> Value) {
  SizeOfList(
    "size",
    listOf("list"),
    FunctionType(listOf(ListType(null)), PrimitiveType.Integer),
    Bifs.sizeOfList
  );
}

object Bifs{
  val sizeOfList: (MutEnv) -> Value =  { params ->
    Value.IntVal(params.getValue("list").listVal().entries.size)
  }
}