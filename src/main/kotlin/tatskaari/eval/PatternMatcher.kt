package tatskaari.eval

import tatskaari.eval.values.Value
import tatskaari.parsing.AssignmentPattern

object PatternMatcher {
  private fun match(value : Value, pattern: AssignmentPattern.Constructor): Boolean{
    return value is Value.VariantVal &&
      value.name == pattern.name.name &&
      match(value.params, pattern.pattern)
  }

  private fun match(value : Value, pattern: AssignmentPattern.Tuple): Boolean{
    return if(value is Value.TupleVal && value.values.size == pattern.identifiers.size){
      !value.values.zip(pattern.identifiers)
        .any { !match(it.first, it.second) }
    } else {
      false
    }
  }

  fun match(value: Value, pattern: AssignmentPattern): Boolean {
    return when (pattern) {
      AssignmentPattern.Unit -> value == Value.Unit
      is AssignmentPattern.Variable -> true
      is AssignmentPattern.Constructor -> match(value, pattern)
      is AssignmentPattern.Tuple -> match(value, pattern)
    }
  }
}