package tokenising

sealed class Operator(val text : String) {
  object Add : Operator("+")
  object Sub : Operator("-")

  override fun toString(): String {
    return text
  }
}