package tatskaari.tokenising

sealed class Operator(val text : String) {
  object Add : Operator("+")
  object Sub : Operator("-")
  object Equality : Operator("=")

  override fun toString(): String {
    return text
  }
}