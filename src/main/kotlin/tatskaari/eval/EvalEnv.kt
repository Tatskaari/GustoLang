package tatskaari.eval

import tatskaari.GustoType
import tatskaari.eval.values.Value

class EvalEnv(val vairableEnv: HashMap<String, Value>, val typeDefinitions: HashMap<String, GustoType>) : MutableMap<String, Value> by vairableEnv {
  constructor(env: EvalEnv) : this(HashMap(env.vairableEnv), HashMap(env.typeDefinitions))
  constructor() : this(HashMap(), HashMap())

  operator fun set(identifierName: String, value: Value) = vairableEnv.put(identifierName, value)
  override operator fun get(key: String) : Value {
    if (vairableEnv.containsKey(key)){
      return vairableEnv.getValue(key)
    } else {
      throw Eval.UndefinedIdentifier(key)
    }
  }

  fun hasVariable(name: String) = vairableEnv.containsKey(name)
}