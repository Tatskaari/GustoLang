package tokenising

sealed class Operator {
  object Add : Operator()
  object Sub : Operator()
}