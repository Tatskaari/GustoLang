package tatskaari.tokenising

sealed class Operator(val text: String, val resultType: ResultType) {
  enum class ResultType {
    BOOLEAN, NUMERIC;
  }

  object Add : Operator("+", ResultType.NUMERIC)
  object Sub : Operator("-", ResultType.NUMERIC)
  object Equality : Operator("=", ResultType.BOOLEAN)

  override fun toString(): String {
    return text
  }
}