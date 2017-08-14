package tatskaari.tokenising

enum class Operator(val text: String) {
  Add("+"),
  Sub("-"),
  Mul("*"),
  Div("/"),
  Equality("="),
  LessThan("<"),
  GreaterThan(">"),
  LessThanEq("<="),
  GreaterThanEq(">=");

  override fun toString(): String {
    return text
  }
}